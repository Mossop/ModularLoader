package com.blueprintit;

import java.net.URL;
import java.net.URLConnection;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.prefs.Preferences;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;

public class DefaultApplication implements Application
{
	Map modules = new HashMap();
	private Map loaded = new HashMap();
	private List listeners = new ArrayList();
	String application;
	URL descriptorURL;
	String initialModule;

	public DefaultApplication()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}
	}

	public Preferences getModuleSystemPreferences(Module module)
	{
		ModuleInfo info = getModuleInfo(module);
		return getApplicationSystemPreferences().node(info.getID());
	}

	Preferences getApplicationSystemPreferences()
	{
		return Preferences.systemNodeForPackage(getClass()).node(application);
	}

	public Preferences getModuleUserPreferences(Module module)
	{
		ModuleInfo info = getModuleInfo(module);
		return getApplicationUserPreferences().node(info.getID());
	}

	Preferences getApplicationUserPreferences()
	{
		return Preferences.userNodeForPackage(getClass()).node(application);
	}

	public ModuleInfo getModuleInfo(String id)
	{
		return (ModuleInfo)modules.get(id);
	}

	public ModuleInfo getModuleInfo(Module m)
	{
		Iterator loop = loaded.entrySet().iterator();
		while (loop.hasNext())
		{
			Map.Entry entry = (Map.Entry)loop.next();
			if (entry.getValue()==m)
			{
				return getModuleInfo((String)entry.getKey());
			}
		}
		return null;
	}

	public boolean isAvailable(ModuleInfo info)
	{
		if (info.localversion>=0)
		{
			Iterator loop = info.requires.iterator();
			while (loop.hasNext())
			{
				ModuleInfo requirement = (ModuleInfo)loop.next();
				if (!isAvailable(requirement))
				{
					return false;
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean isLoaded(ModuleInfo info)
	{
		return getModule(info)!=null;
	}

	private boolean download(ModuleInfo info)
	{
		try
		{
			File target = new File("modules",info.url.replace('/',System.getProperty("file.separator").charAt(0)));
			URL source = new URL(descriptorURL,info.url);
			URLConnection connection = source.openConnection();
			connection.connect();
			int size = connection.getContentLength();
			ProgressMonitor progress = new ProgressMonitor(null,"Downloading "+info.getTitle(),null,0,size);
			progress.setProgress(0);
			progress.setMillisToDecideToPopup(100);
			progress.setMillisToPopup(0);
			InputStream in = connection.getInputStream();
			byte[] buffer = new byte[1024];
			int count=0;
			int len = in.read(buffer);
			OutputStream out = new FileOutputStream(target);
			while (len>0)
			{
				out.write(buffer,0,len);
				try
				{
					Thread.sleep(100);
				}
				catch (Exception e)
				{
				}
				count+=len;
				progress.setProgress(count);
				if (progress.isCanceled())
				{
					progress.close();
					in.close();
					out.close();
					target.delete();
					info.localfile=null;
					info.localversion=-1;
					return false;
				}
				len=in.read(buffer);
			}
			progress.close();
			in.close();
			out.close();
			info.localfile=target;
			info.localversion=info.remoteversion;
			Preferences modprefs = getApplicationSystemPreferences().node("descriptorcache/"+info.getID());
			modprefs.put("localfile",target.getAbsolutePath());
			modprefs.putFloat("localversion",info.localversion);
			modprefs.flush();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public boolean makeAvailable(ModuleInfo info)
	{
		if (isAvailable(info))
		{
			return true;
		}
		else
		{
			Iterator loop = info.requires.iterator();
			while (loop.hasNext())
			{
				ModuleInfo dep = (ModuleInfo)loop.next();
				if (!makeAvailable(dep))
				{
					return false;
				}
			}
			return download(info);
		}
	}

	public Module getModule(ModuleInfo info)
	{
		return (Module)loaded.get(info.getID());
	}

	public Module loadModule(ModuleInfo info)
	{
		Module module = getModule(info);
		if (module==null)
		{
			if (isAvailable(info))
			{
				ClassLoader dependencies[] = new ClassLoader[info.requires.size()];
				int count=0;
				Iterator loop = info.requires.iterator();
				while (loop.hasNext())
				{
					dependencies[count]=loadModule((ModuleInfo)loop.next()).getClass().getClassLoader();
					count++;
				}
				try
				{
					ClassLoader loader = new JarModuleLoader(getClass().getClassLoader(),dependencies,info.localfile);
					Class modclass = loader.loadClass(info.classname);
					module = (Module)modclass.newInstance();
					loaded.put(info.getID(),module);
					module.initialise();
					return module;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return null;
				}
			}
			return null;
		}
		return module;
	}

	void startup()
	{
		if ((initialModule!=null)&&(modules.get(initialModule)!=null))
		{
			ModuleInfo start = (ModuleInfo)modules.get(initialModule);
			if (makeAvailable(start))
			{
				ModuleInfo info;
				Iterator loop = modules.values().iterator();
				while (loop.hasNext())
				{
					info = (ModuleInfo)loop.next();
					if (info.autoload)
					{
						loadModule(info);
					}
				}
				loadModule(start);
			}
			else
			{
				System.out.println("Unable to download startup module");
				System.exit(0);
			}
		}
		else
		{
			System.out.println("Unable to locate startup module");
		}
	}

	public void exit()
	{
		Iterator loop = loaded.values().iterator();
		while (loop.hasNext())
		{
			Module mod = (Module)loop.next();
			mod.destroy();
		}
		System.exit(0);
	}

	public void addModuleListener(ModuleListener l)
	{
		synchronized(listeners)
		{
			listeners.add(l);
		}
	}

	public void removeModuleListener(ModuleListener l)
	{
		synchronized(listeners)
		{
			listeners.remove(l);
		}
	}

	protected void deliverEvent(ModuleEvent e)
	{
		List newlist;
		synchronized(listeners)
		{
			newlist = new ArrayList(listeners);
		}
		Iterator loop = newlist.iterator();
		while (loop.hasNext())
		{
			ModuleListener l = (ModuleListener)loop.next();
			if (e.getId()==e.MODULELOADED)
			{
				l.moduleLoaded(e);
			}
			else if (e.getId()==e.MODULEUNLOADED)
			{
				l.moduleUnloaded(e);
			}
		}
	}
}
