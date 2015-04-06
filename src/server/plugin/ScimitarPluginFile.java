package server.plugin;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.zip.*;

/**
 *  Copyright (c) 2014, john01dav
	All rights reserved.
	
	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions are met:
	    * Redistributions of source code must retain the above copyright
	      notice, this list of conditions and the following disclaimer.
	    * Redistributions in binary form must reproduce the above copyright
	      notice, this list of conditions and the following disclaimer in the
	      documentation and/or other materials provided with the distribution.
	    * Neither the name of the <organization> nor the
	      names of its contributors may be used to endorse or promote products
	      derived from this software without specific prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
	ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
	WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
	DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
	(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
	ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
	(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
 * @author john01dav
 *
 */
public class ScimitarPluginFile {
	private URL url;
	private File file;
	private ZipFile jarFile;
	private ZipEntry jarEntry;
	private InputStream fileStream;
	private Properties properties;

	public ScimitarPluginFile(File file) {
		this.file = file;
		this.properties = new Properties();
	}

	public ScimitarPluginFile load() {
		try {
			url = new URL("file://" + file.getAbsolutePath());
			jarFile = new ZipFile(file.getAbsolutePath());
			jarEntry = jarFile.getEntry("plugindata.txt");
			fileStream = jarFile.getInputStream(jarEntry);
			properties.load(fileStream);
			fileStream.close();
			jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	public File getFile() {
		return file;
	}

	public URL getURL() {
		return url;
	}

	public String getName() {
		return properties.getProperty("name");
	}

	public String getMainClass() {
		return properties.getProperty("mainClass");
	}

}