package com.blueprintit;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class ModuleLoader extends ClassLoader
{
	protected ClassLoader[] parents;

	public ModuleLoader(ClassLoader parent, ClassLoader[] parents)
	{
		super(parent);
		this.parents=parents;
	}

	protected Class findClass(String name) throws ClassNotFoundException
	{
		for (int loop=0; loop<parents.length; loop++)
		{
			try
			{
				Class newclass=parents[loop].loadClass(name);
				return newclass;
			}
			catch (ClassNotFoundException e)
			{
			}
		}
		try
		{
			byte[] data = loadClassData(name);
			return defineClass(name,data,0,data.length);
		}
		catch (Exception e)
		{
			throw new ClassNotFoundException("Unable to load class "+name,e);
		}
	}

	protected byte[] loadClassData(String name) throws Exception
	{
		InputStream in = getResourceAsStream(name.replace('.','/')+".class");
		if (in!=null)
		{
			byte[] buffer = new byte[1024];
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int len = in.read(buffer);
			while (len>0)
			{
				out.write(buffer,0,len);
				len=in.read(buffer);
			}
			out.flush();
			buffer = out.toByteArray();
			out.close();
			in.close();
			return buffer;
		}
		else
		{
			throw new IOException("Unable to load class data for class "+name);
		}
	}
}
