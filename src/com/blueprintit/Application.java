package com.blueprintit;

import java.util.prefs.Preferences;

public interface Application
{
	public Preferences getModuleSystemPreferences(Module module);

	public Preferences getModuleUserPreferences(Module module);

	public ModuleInfo getModuleInfo(String id);

	public ModuleInfo getModuleInfo(Module m);

	public boolean isAvailable(ModuleInfo info);

	public boolean isLoaded(ModuleInfo info);

	public boolean makeAvailable(ModuleInfo info);

	public Module getModule(ModuleInfo info);

	public Module loadModule(ModuleInfo info);

	public void exit();

	public void addModuleListener(ModuleListener l);

	public void removeModuleListener(ModuleListener l);
}
