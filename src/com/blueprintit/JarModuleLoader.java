package com.blueprintit;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.io.File;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class JarModuleLoader extends ModuleLoader
{
	private JarFile jarfile;
	private String url;

	public JarModuleLoader(ClassLoader parent, ClassLoader[] parents, File jarfile) throws IOException
	{
		super(parent,parents);
		this.jarfile = new JarFile(jarfile);
		url = jarfile.toURL().toString();
	}

	public URL getResource(String resource)
	{
		URL result;
		for (int loop=0; loop<parents.length; loop++)
		{
			result = parents[loop].getResource(resource);
			if (result!=null)
			{
				return result;
			}
		}
		result = getParent().getResource(resource);
		if (result!=null)
		{
			return result;
		}
		JarEntry entry = jarfile.getJarEntry(resource);
		if (entry!=null)
		{
			try
			{
				result = new URL("jar:"+url+"!/"+resource);
				return result;
			}
			catch (Exception e)
			{
			}
	  }
		return null;
	}

  public InputStream getResourceAsStream(String resource)
  {
  	URL res = getResource(resource);
  	if (res!=null)
  	{
  		try
  		{
	  		return res.openStream();
	  	}
	  	catch (IOException e)
	  	{
	  	}
  	}
  	return null;
  }
}
