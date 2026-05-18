package me.kwilson272.elementalmagic.core.activation;

import java.lang.constant.MethodHandleDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.ability.SequenceController;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.ActionRecord;
import me.kwilson272.elementalmagic.api.activation.Activation;
import me.kwilson272.elementalmagic.api.activation.ActivationManager;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.activation.activations.PassiveActivation;
import me.kwilson272.elementalmagic.api.event.ability.AbilityStartEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

public class ActivationManagerImpl implements ActivationManager {

    private static final int MAX_ACTIONS = 16;
    
    private final SequenceTree sequenceTree;
    private final Map<AbilityUser, Deque<ActionRecord>> actionTrackers;
    private final Map<Class<? extends Activation>, ActivationDispatcher> dispatchers;

    public ActivationManagerImpl() {
        sequenceTree = new SequenceTree();
        actionTrackers = new HashMap<>();
        dispatchers = new HashMap<>();
    }

	@Override
	public void enable() {
	}

	@Override
	public void disable(boolean shutDown) {
        actionTrackers.clear();
	}

	@Override
	public void registerUser(AbilityUser user) {
        getActionTracker(user);
	}

    private Deque<ActionRecord> getActionTracker(AbilityUser user) {
        if (!actionTrackers.containsKey(user)) {
            actionTrackers.put(user, new ArrayDeque<>(MAX_ACTIONS));
        }
        return actionTrackers.get(user);
    }

	@Override
	public void unregisterUser(AbilityUser user) {
        actionTrackers.remove(user);
	}

	@Override
	public void registerController(AbilityController controller) {
        parseActivators(controller);

        if (controller instanceof SequenceController sController) {
            List<ActionRecord> steps = sController.steps();
            sequenceTree.insert(steps.reversed().iterator(), sController);
        }
	}

    private void parseActivators(AbilityController controller) {
        Method[] methods = controller.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(Activator.class) 
                    || !checkValidActivator(method)) {
                continue;
            }

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                method.setAccessible(true);
                
                Activator activator = method.getAnnotation(Activator.class);
                boolean reqSelected = activator.requireSelected();
                boolean reqElement = activator.requireElement();
                MethodHandle handle = lookup.unreflect(method);
        
                Class<?>[] params = method.getParameterTypes();
                var aParam = (Class<? extends Activation>) params[1];
                
                dispatchers.computeIfAbsent(aParam, k -> new ActivationDispatcher())
                    .insert(controller, handle, reqElement, reqSelected);
            
            } catch (IllegalAccessException e) {
                String mName = method.getName();
                String cName = method.getDeclaringClass().getName();
                Logger logger = ElementalMagicApi.logger();
                logger.warning("Unable to register '" + mName + "' in '" + cName + "' as an activator.");
            }
        }
    }

    private boolean checkValidActivator(Method method) {
        List<String> errors = new ArrayList<>();
        if (!hasValidReturnType(method)) {
            errors.add("Invalid return type; expected Collection<Ability>.");
        }
        if (!hasValidParamList(method)) {
            errors.add("Invalid parameter types; expected AbilityUser and Activation sub-class"); 
        }
        
        if (!errors.isEmpty()) {
            String mName = method.getName();
            String cName = method.getDeclaringClass().getName();
            Logger logger = ElementalMagicApi.logger();
            logger.warning("Cannot register activator '" + mName + "' for '" + cName + "':");
            errors.forEach(logger::warning);
            return false;
        }
        return true;
    }
    
    private boolean hasValidReturnType(Method method) {
        Type rType = method.getGenericReturnType();
        return rType instanceof ParameterizedType pType 
            && pType.getRawType() == Collection.class
            && pType.getActualTypeArguments().length == 1
            && pType.getActualTypeArguments()[0] == Ability.class;
    }

    private boolean hasValidParamList(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return params.length == 2
            && AbilityUser.class == params[0]
            && Activation.class.isAssignableFrom(params[1]);
    }

	@Override
	public void postActivation(AbilityUser user, Activation activation) {
        ActivationDispatcher dispatcher = dispatchers.get(activation.getClass());
        if (dispatcher == null) {
            return;
        }

        for (ReferencedActivator activator : dispatcher.get(user)) {
            AbilityController ref = activator.controller; 
            MethodHandle handle = activator.handle;
            
            try {
                Collection<Ability> instances = 
                    (Collection<Ability>) handle.invoke(ref, user, activation);
                instances.forEach(this::processAbilityCreation);
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }
	}

    private void processAbilityCreation(Ability ability) {
        var startEvent = new AbilityStartEvent(ability);
        if (!startEvent.isCancelled() && ability.start()) {
            ElementalMagicApi.abilityManager().addAbility(ability);  
        }
    }

	@Override
	public void handleAction(AbilityUser user, Action action) {
        Deque<ActionRecord> actions = getActionTracker(user);
        user.getSelectedBind().ifPresent(a -> {
            ActionRecord record = new ActionRecord(a.name(), action);
            actions.offerFirst(record);
        });
        
        while (actions.size() > MAX_ACTIONS) {
            actions.pollLast();
        }

        Iterator<ActionRecord> actionIter = actions.iterator();
        SequenceController controller = sequenceTree.query(actionIter);
        if (controller != null) {
            Collection<Ability> instances = controller.createAbilities(user);
            instances.forEach(this::processAbilityCreation);
        }
	}

	@Override
	public void clearTrackedActions(AbilityUser user) {
        getActionTracker(user).clear();

	}

    @Override
    public void createPassives() {
        PassiveActivation activation = new PassiveActivation();
        for (AbilityUser user : ElementalMagicApi.userManager().getAll()) {
            postActivation(user, activation);
        }
    }

    private class ActivationDispatcher {
        
        private final List<ReferencedActivator> noReqs;
        private final Multimap<Element, ReferencedActivator> reqElement;
        private final Multimap<AbilityController, ReferencedActivator> reqSelected;

        ActivationDispatcher() {
            noReqs = new ArrayList<>();
            reqElement = MultimapBuilder.hashKeys().arrayListValues().build();
            reqSelected = MultimapBuilder.hashKeys().arrayListValues().build();
        }
        
        void insert(AbilityController controller, MethodHandle handle,
                    boolean requireElement, boolean requireSelected) {
            var activator = new ReferencedActivator(controller, handle);

            if (requireSelected) {
                reqSelected.put(controller, activator);
            } else if (requireElement) {
                reqElement.put(controller.element(), activator);
            } else {
                noReqs.add(activator);
            }
        }
       
        List<ReferencedActivator> get(AbilityUser user) {
            List<ReferencedActivator> found = new ArrayList<>();
            found.addAll(noReqs);

            user.getSelectedBind()
                .ifPresent(c -> found.addAll(reqSelected.get(c)));

            for (Element element : user.getElements()) {
                found.addAll(reqElement.get(element));
            }

            return found;
        }
    }

    private record ReferencedActivator(AbilityController controller, 
                                       MethodHandle handle) {}

    private class SequenceTree {

        private final Map<ActionRecord, SequenceTree> children;
        private SequenceController controller;

        SequenceTree() {
            children = new HashMap<>();
            controller = null;
        }

        void insert(Iterator<ActionRecord> steps, SequenceController controller) {
            if (!steps.hasNext()) {
                this.controller = controller;
                return;
            }

            ActionRecord nextAction = steps.next();
            if (!children.containsKey(nextAction)) {
                children.put(nextAction, new SequenceTree());
            }

            children.get(nextAction).insert(steps, controller);
        }

        SequenceController query(Iterator<ActionRecord> steps) {
            if (controller != null) {
                return controller;
            } else if (!steps.hasNext()) {
                return null;
            }

            ActionRecord nextAction = steps.next();
            if (children.containsKey(nextAction)) {
                return children.get(nextAction).query(steps);
            } 

            return null;
        }
    }
}
