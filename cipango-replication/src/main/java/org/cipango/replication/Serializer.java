// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.replication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

public class Serializer
{
	public static byte[] serialize(Object o) throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(o);
		return os.toByteArray();
	}
	
	public static Object deserialize(byte[] b) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream is = new ByteArrayInputStream(b);
		ObjectInputStream ois = new ContextObjectInputStream(is);
		Object o = ois.readObject();
		ois.close();
		return o;
	}
	
	public static class ContextObjectInputStream extends ObjectInputStream
	{

		public ContextObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
				ClassNotFoundException {
			String name = desc.getName();
	        try {
	            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
	        } catch (ClassNotFoundException ex) {
	           return super.resolveClass(desc);
	        }
		}
		
	}
	
}
