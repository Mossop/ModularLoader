package com.blueprintit;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class ModuleInfo
{
	private String id;
	boolean autoload;
	String classname;
	String url;
	File localfile;
	String title;
	float remoteversion;
	float localversion=-1;
	List requires = new ArrayList();
	List requiredby = new ArrayList();

	public ModuleInfo(String id)
	{
		this.id=id;
	}

	public String getID()
	{
		return id;
	}

	public String getTitle()
	{
		return title;
	}

	public boolean updateAvailable()
	{
		return remoteversion>localversion;
	}
}
