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

public class ModularApplication
{
	private static DefaultApplication app;

	private ModularApplication()
	{
		app=new DefaultApplication();
		loadData();
		app.startup();
	}

	public static Application getApplication()
	{
		return app;
	}

	private void loadCachedDescriptor()
	{
		try
		{
			Preferences cache = app.getApplicationSystemPreferences();
			if (cache.nodeExists("descriptorcache"))
			{
				synchronized(app.modules)
				{
					app.modules.clear();
					cache=cache.node("descriptorcache");
					app.initialModule=cache.get("initialModule",null);
					String[] mods = cache.childrenNames();
					for (int loop=0; loop<mods.length; loop++)
					{
						Preferences modprefs = cache.node(mods[loop]);
						ModuleInfo module = new ModuleInfo(mods[loop]);
						module.classname=modprefs.get("class",null);
						module.url=modprefs.get("url",null);
						module.autoload=modprefs.getBoolean("autoload",false);
						module.title=modprefs.get("title","");
						if ((module.url!=null)&&(module.classname!=null))
						{
							module.localversion=modprefs.getFloat("localversion",-1);
							if (module.localversion>=0)
							{
								module.localfile=new File(modprefs.get("localfile",null));
								if (!module.localfile.exists())
								{
									module.localfile=null;
									module.localversion=-1;
								}
							}
							module.remoteversion=modprefs.getFloat("remoteversion",-1);
							String[] reqs = modprefs.get("requires","").split(",");
							for (int count=0; count<reqs.length; count++)
							{
								ModuleInfo dep = (ModuleInfo)app.modules.get(reqs[count]);
								if (dep!=null)
								{
									module.requires.add(dep);
									dep.requiredby.add(module);
								}
							}
							app.modules.put(module.getID(),module);
						}
					}
				}
			}
			else
			{
				System.err.println("No cached descriptor");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error loading cached descriptor");
		}
	}

	private void loadDescriptor()
	{
		System.out.println("Loading description for "+app.application+" from "+app.descriptorURL);
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(app.descriptorURL.openStream()));
			String line = in.readLine();
			String[] header = line.split(":",3);
			if ((header.length==3)&&(header[0].equals("MAD1")))
			{
				app.initialModule=header[1];
				URL test = new URL(header[2]);
				if (test.equals(app.descriptorURL))
				{
					ModuleInfo info = null;
					Preferences moduleprefs;
					line=in.readLine();
					synchronized(app.modules)
					{
						app.modules.clear();
						while (line!=null)
						{
							line=line.trim();
							if (line.length()>0)
							{
								if ((line.charAt(0)=='[')&&(line.charAt(line.length()-1)==']'))
								{
									line=line.substring(1,line.length()-1);
									info = new ModuleInfo(line);
									app.modules.put(line,info);
									moduleprefs = app.getApplicationSystemPreferences().node("descriptorcache/"+line);
									info.localversion=moduleprefs.getFloat("localversion",-1);
									if (info.localversion>=0)
									{
										String local = moduleprefs.get("localfile",null);
										if (local!=null)
										{
											info.localfile=new File(local);
											if (!info.localfile.exists())
											{
												info.localfile=null;
												info.localversion=-1;
											}
										}
										else
										{
											info.localfile=null;
											info.localversion=-1;
										}
									}
								}
								else
								{
									int pos = line.indexOf("=");
									String key = line.substring(0,pos);
									String value = line.substring(pos+1);
									if (key.equals("URL"))
									{
										info.url=value;
									}
									else if (key.equals("title"))
									{
										info.title=value;
									}
									else if (key.equals("autoload"))
									{
										info.autoload=Boolean.valueOf(value).booleanValue();
									}
									else if (key.equals("class"))
									{
										info.classname=value;
									}
									else if (key.equals("version"))
									{
										info.remoteversion=Float.parseFloat(value);
									}
									else if (key.equals("requires"))
									{
										String[] ids = value.split(",");
										for (int loop=0; loop<ids.length; loop++)
										{
											ModuleInfo dep = (ModuleInfo)app.modules.get(ids[loop]);
											if ((dep!=null)&&(dep!=info))
											{
												dep.requiredby.add(info);
												info.requires.add(dep);
											}
										}
									}
								}
							}
							line=in.readLine();
						}
						Preferences prefs = app.getApplicationSystemPreferences().node("descriptorcache");
						prefs.clear();
						String[] oldchildren = prefs.childrenNames();
						for (int count=0; count<oldchildren.length; count++)
						{
							prefs.node(oldchildren[count]).removeNode();
						}
						prefs.flush();
						prefs.put("initialModule",app.initialModule);
						Iterator loop = app.modules.values().iterator();
						while (loop.hasNext())
						{
							info = (ModuleInfo)loop.next();
							if ((info.url!=null)&&(info.classname!=null))
							{
								moduleprefs = prefs.node(info.getID());
								moduleprefs.put("url",info.url.toString());
								moduleprefs.put("class",info.classname);
								moduleprefs.put("title",info.title);
								moduleprefs.putBoolean("autoload",info.autoload);
								moduleprefs.putFloat("localversion",info.localversion);
								moduleprefs.putFloat("remoteversion",info.remoteversion);
								String requires = "";
								Iterator reqloop = info.requires.iterator();
								while (reqloop.hasNext())
								{
									ModuleInfo mod = (ModuleInfo)reqloop.next();
									requires+=","+mod.getID();
								}
								if (requires.length()>0)
								{
									moduleprefs.put("requires",requires.substring(1));
								}
								moduleprefs.flush();
							}
							else
							{
								//loop.remove();
							}
						}
					}
				}
				else
				{
					System.out.println("Application descriptor has moved to "+test);
					app.descriptorURL=test;
					loadDescriptor();
				}
			}
			else
			{
				System.err.println("Invalid application descriptor found");
				loadCachedDescriptor();
			}
			in.close();
		}
		catch (Exception e)
		{
			System.err.println("Unable to download application descriptor");
			loadCachedDescriptor();
		}
	}

	private void loadData()
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("modularapplication.properties")));
			String line = in.readLine();
			while (line!=null)
			{
				int pos = line.indexOf("=");
				if (pos>0)
				{
					String key = line.substring(0,pos);
					String value = line.substring(pos+1);
					if (key.equals("application"))
					{
						app.application=value;
					}
					else if (key.equals("descriptorURL"))
					{
						try
						{
							app.descriptorURL = new URL(value);
						}
						catch (Exception e)
						{
						}
					}
				}
				line=in.readLine();
			}
			loadDescriptor();
		}
		catch (Exception e)
		{
			System.err.println("Unable to load application properties");
			app.application=System.getProperty("application",null);
			try
			{
				app.descriptorURL = new URL(System.getProperty("descriptorURL",null));
				loadDescriptor();
			}
			catch (Exception ee)
			{
			}
		}
	}

	public static void main(String[] args)
	{
		ModularApplication ma = new ModularApplication();
	}
}
