package com.blueprintit;

public interface ModuleListener
{
	public void moduleLoaded(ModuleEvent e);

	public void moduleUnloaded(ModuleEvent e);
}
