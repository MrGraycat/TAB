package me.neznamy.tab.shared;

import java.util.ArrayList;
import java.util.List;

import me.neznamy.tab.shared.placeholders.ServerConstant;
import me.neznamy.tab.shared.placeholders.Placeholder;
import me.neznamy.tab.shared.placeholders.Placeholders;

public class Property {

	private ITabPlayer owner;
	private String rawValue;
	private String temporaryValue;
	public String lastReplacedValue;
	private String source;

	private List<Placeholder> placeholders = new ArrayList<Placeholder>();
	private boolean hasRelationalPlaceholders;
	private long lastUpdate;
	private boolean Static;

	public Property(ITabPlayer owner, String rawValue, String source) {
		if (rawValue == null) rawValue = "";
		this.owner = owner;
		this.source = source;
		this.rawValue = analyze(rawValue);
	}
	private String analyze(String value) {
		for (Placeholder c : Placeholders.getAllPlaceholders()) {
			if (c instanceof ServerConstant && value.contains(c.getIdentifier())) {
				value = value.replace(c.getIdentifier(), ((ServerConstant)c).get());
			}
		}
		placeholders = Placeholders.detectPlaceholders(value);
		hasRelationalPlaceholders = value.contains("%rel_");
		for (Placeholder placeholder : placeholders) {
			for (String child : placeholder.getChilds()) {
				if (String.valueOf(child).contains("%rel_")) hasRelationalPlaceholders = true;
			}
		}
		if (placeholders.isEmpty() && !hasRelationalPlaceholders) {
			//no placeholders, this is a static string
			//performing final changes before saving it
			for (String removed : Configs.removeStrings) {
				if (value.contains(removed)) value = value.replace(removed, "");
			}
			lastReplacedValue = Placeholders.color(value);
			Static = true;
		} else {
			lastReplacedValue = null;
			Static = false;
		}
		return value;
	}
	public void setTemporaryValue(String temporaryValue) {
		this.temporaryValue = temporaryValue;
		if (temporaryValue != null) {
			temporaryValue = analyze(temporaryValue);
		} else {
			rawValue = analyze(rawValue);
		}
	}
	public void changeRawValue(String newValue) {
		if (rawValue.equals(newValue)) return;
		rawValue = newValue;
		if (temporaryValue == null) {
			rawValue = analyze(rawValue);
		}
	}
	public String get() {
		if (lastReplacedValue == null) isUpdateNeeded();
		return lastReplacedValue;
	}
	public String getCurrentRawValue() {
		return temporaryValue != null ? temporaryValue : rawValue;
	}
	public String getTemporaryValue() {
		return temporaryValue;
	}
	public String getOriginalRawValue() {
		return rawValue;
	}
	public String getSource() {
		return temporaryValue == null ? source : "API";
	}
	public void setSource(String source) {
		this.source = source;
	}
	public boolean isUpdateNeeded() {
		if (Static) return false;
		String string = getCurrentRawValue();

		//placeholders
		for (Placeholder pl : placeholders) {
			string = pl.set(string, owner);
		}
		
		string = Placeholders.color(string);
		
		//removing strings
		for (String removed : Configs.removeStrings) {
			if (string.contains(removed)) string = string.replace(removed, "");
		}
		if (lastReplacedValue == null || !string.equals(lastReplacedValue) || (hasRelationalPlaceholders() && System.currentTimeMillis()-lastUpdate > (float) Configs.SECRET_relational_placeholders_refresh *1000)) {
			lastReplacedValue = string;
			lastUpdate = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}
	}
	public boolean hasRelationalPlaceholders() {
		return hasRelationalPlaceholders && PluginHooks.placeholderAPI;
	}
	public List<Placeholder> getUsedPlaceholders(){
		return placeholders;
	}
}