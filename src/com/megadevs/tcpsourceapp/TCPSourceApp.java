/***********************************************************************
 * 
 * Copyright (c) 2013, Sebastiano Gottardo
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the MegaDevs nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SEBASTIANO GOTTARDO BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.megadevs.tcpsourceapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class TCPSourceApp {

	public static class AppDescriptor {

		private String packageName;
		private String version;
		private String baseURL;
		
		public AppDescriptor(String pName, String ver, String base) {
			packageName = pName;
			version = ver;
			baseURL = base;
		}
		
		public String getPackageName() {
			return packageName;
		}
		
		public String getVersion() {
			return version;
		}
		
		public String getBaseURL() {
			return baseURL;
		}
		
		public void setBaseURL(String base) {
			baseURL = base;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof AppDescriptor) {
				boolean c1 = ((AppDescriptor) o).packageName.compareTo(this.packageName) == 0;
				boolean c2 = ((AppDescriptor) o).version.compareTo(this.version) == 0;
				boolean c3 = ((AppDescriptor) o).baseURL.compareTo(this.baseURL) == 0;
				
				return c1 && c2 && c3;
			}
		
			return false;
		}
		
	}
	
	private static final String TCP_4_FILE_PATH 	= "/proc/net/tcp";
	private static final String TCP_6_FILE_PATH 	= "/proc/net/tcp6";
	
	// (address) (port) (pid)
	private static final String TCP_6_PATTERN 	= "\\d:\\s([0-9A-F]{32}):([0-9A-F]{4})\\s[0-9A-F]{32}:[0-9A-F]{4}\\s[0-9A-F]{2}\\s[0-9]{8}:[0-9]{8}\\s[0-9]{2}:[0-9]{8}\\s[0-9]{8}\\s([0-9]+)";
	
	// (address) (port) (pid)
	private static final String TCP_4_PATTERN 	= "\\d:\\s([0-9A-F]{8}):([0-9A-F]{4})\\s[0-9A-F]{8}:[0-9A-F]{4}\\s[0-9A-F]{2}\\s[0-9A-F]{8}:[0-9A-F]{8}\\s[0-9]{2}:[0-9]{8}\\s[0-9A-F]{8}\\s\\s([0-9]+)";

	
	@SuppressWarnings("unused")
	public static AppDescriptor getPackageFromPort(Context context, int port) {
		try {
			
			String ipv4Address = getIPAddress(true);
			String ipv6Address = getIPAddress(false);

			boolean hasIPv6 = (ipv6Address.length() > 0); //TODO use this value to skip ipv6 check, eventually
			
			File tcp = new File(TCP_6_FILE_PATH);
			BufferedReader reader = new BufferedReader(new FileReader(tcp));
			String line = "";
			StringBuilder builder = new StringBuilder();
			
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			String content = builder.toString();
			
			Matcher m6 = Pattern.compile(TCP_6_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(content);
			
			while (m6.find()) {
				String addressEntry = m6.group(1);
				String portEntry 	= m6.group(2);
				int pidEntry 	= Integer.valueOf(m6.group(3));
				
				if (Integer.parseInt(portEntry, 16) == port) {
					
					PackageManager manager = context.getPackageManager();
					String[] packagesForUid = manager.getPackagesForUid(pidEntry);
					
					if (packagesForUid != null) {
						String packageName = packagesForUid[0];
						PackageInfo pInfo = manager.getPackageInfo(packageName, 0);
						String version = pInfo.versionName;

						return new AppDescriptor(packageName, version, null);
					}
				}
				
			}
			
			// this means that no connection with that port could be found in the tcp6 file
			// try the tcp one
			
			tcp = new File(TCP_4_FILE_PATH);
			reader = new BufferedReader(new FileReader(tcp));
			line = "";
			builder = new StringBuilder();
			
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			content = builder.toString();
			
			Matcher m4 = Pattern.compile(TCP_4_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL).matcher(content);
			
			while (m4.find()) {
				String addressEntry = m4.group(1);
				String portEntry 	= m4.group(2);
				int pidEntry 	= Integer.valueOf(m4.group(3));
				
				portEntry = convertHexToString(portEntry); // hex to ascii
				
				if (Integer.valueOf(portEntry) == port) {
					PackageManager manager = context.getPackageManager();
					String[] packagesForUid = manager.getPackagesForUid(pidEntry);
					
					if (packagesForUid != null) {
						String packageName = packagesForUid[0];
						PackageInfo pInfo = manager.getPackageInfo(packageName, 0);
						String version = pInfo.versionName;

						return new AppDescriptor(packageName, version, null);
					}
				}
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	private static String convertHexToString(String hex) {
		 
		  StringBuilder sb = new StringBuilder();
		  StringBuilder temp = new StringBuilder();
	 
		  //49204c6f7665204a617661 split into two characters 49, 20, 4c...
		  for( int i=0; i<hex.length()-1; i+=2 ){
	 
		      //grab the hex in pairs
		      String output = hex.substring(i, (i + 2));
		      //convert hex to decimal
		      int decimal = Integer.parseInt(output, 16);
		      //convert the decimal to character
		      sb.append((char)decimal);
	 
		      temp.append(decimal);
		  }
	 
		  return sb.toString();
	  }
	
	
	@SuppressLint("DefaultLocale")
	public static String getIPAddress(boolean useIPv4) throws SocketException {
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
			for (InetAddress addr : addrs) {
				if (!addr.isLoopbackAddress()) {
					String sAddr = addr.getHostAddress().toUpperCase();
					boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
					
					if (useIPv4) {
						if (isIPv4) 
							return sAddr;
					} else {
						if (!isIPv4) {
							if (sAddr.startsWith("fe80") || sAddr.startsWith("FE80")) // skipping link-local addresses
								continue;
							
							int delim = sAddr.indexOf('%'); // drop ip6 port suffix
							return delim<0 ? sAddr : sAddr.substring(0, delim);
						}
					}
				}
			}
		}
		
		return "";
	}

	
}
