package me.kwilson272.elementalmagic.core.ability;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.ability.Element;

public class AbilityStorageImpl implements AbilityStorage {
    
    public Set<Element> elements;
    public Map<String, Element> elementsByAlias;
    public Multimap<Element, Element> elementsByParent;
    
    public AbilityStorageImpl() {
        elements = new HashSet<>();
        elementsByAlias = new HashMap<>();
        elementsByParent = MultimapBuilder.hashKeys().arrayListValues().build();
    }

	@Override
	public void enable() {
	}

	@Override
	public void disable(boolean shutDown) {
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
        return elementsByParent.get(parent);
	}

	@Override
	public Optional<Element> getElement(String search) {
        Element element = elementsByAlias.get(search.toUpperCase());
        return Optional.ofNullable(element);
	}
}
