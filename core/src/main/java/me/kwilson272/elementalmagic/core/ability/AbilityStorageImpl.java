package me.kwilson272.elementalmagic.core.ability;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.ability.Element;

public class AbilityStorageImpl implements AbilityStorage {
   
    public Set<AbilityController> controllers;
    public Map<String, AbilityController> controllersByName;
    public Multimap<Element, AbilityController> controllersByElement;

    public Set<Element> elements;
    public Map<String, Element> elementsByAlias;
    public Multimap<Element, Element> elementsByParent;
    
    public AbilityStorageImpl() {
        controllers = new HashSet<>();
        controllersByName = new HashMap<>();
        controllersByElement = MultimapBuilder.hashKeys().arrayListValues().build();
        elements = new HashSet<>();
        elementsByAlias = new HashMap<>();
        elementsByParent = MultimapBuilder.hashKeys().arrayListValues().build();
    }

	@Override
	public void enable() {
	}

	@Override
	public void disable(boolean shutDown) {
        controllers.clear();
        controllersByName.clear();
        controllersByElement.clear();
        elements.clear();
        elementsByAlias.clear();
        elementsByParent.clear();
	}
   
	@Override
	public void registerController(AbilityController controller) {
        if (controllers.contains(controller)) {
            return;
        }

        controllers.add(controller);
        controllersByName.put(controller.name().toUpperCase(), controller);
        controllersByElement.put(controller.element(), controller);
	}

	@Override
	public Collection<AbilityController> getControllers() {
        return Set.copyOf(controllers);
	}

	@Override
	public Collection<AbilityController> getControllersByElement(Element element) {
        return Set.copyOf(controllersByElement.get(element));
	}

    @Override
    public Optional<AbilityController> getController(String name) {
        return Optional.ofNullable(controllersByName.get(name.toUpperCase()));
    }

    @Override
    public void registerElement(Element element) {
        if (elements.contains(element)) {
            return;
        }

        elements.add(element);
        for (String alias : element.aliases()) {
            elementsByAlias.put(alias.toLowerCase(), element);
        }
        for (Element parent : element.parents()) {
            elementsByParent.put(parent, element);
        }
    }

	@Override
	public Collection<Element> getElements() {
        return Set.copyOf(elements);
	}

	@Override
	public Collection<Element> getElementsByParent(Element parent) {
        return Set.copyOf(elementsByParent.get(parent));
	}

	@Override
	public Optional<Element> getElement(String search) {
        Element element = elementsByAlias.get(search.toUpperCase());
        return Optional.ofNullable(element);
	}
}
