package com.blueprintit;

public class ModuleEvent
{
	private Module source;
	private int id;

	public static final int MODULELOADED = 1;
	public static final int MODULEUNLOADED = 2;

	public ModuleEvent(Module source, int id)
	{
		this.source=source;
		this.id=id;
	}

	public String toString()
	{
		return "ModuleEvent "+id+" from "+source;
	}

	public Module getSource()
	{
		return source;
	}

	public int getId()
	{
		return id;
	}
}
