/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author katkav
 * @author mederly
 */
public class CanonicalItemPath implements Serializable {

	private static final String SHORTCUT_MARKER = "$";

	// currently we support only named segments in canonical paths
	public static class Segment implements Serializable {
		private final QName name;
		private final Integer index;		// N means this is Nth unique non-empty namespace in the path (starting from 0)
		private final Integer shortcut;		// K means this namespace is the same as the one with index=K (null if 1st occurrence)
		private Segment(QName name, Integer index, Integer shortcut) {
			this.name = name;
			this.index = index;
			this.shortcut = shortcut;
		}
		public QName getName() {
			return name;
		}
		public Integer getIndex() {
			return index;
		}
		public Integer getShortcut() {
			return shortcut;
		}
	}

	private final List<Segment> segments = new ArrayList<>();

	public static CanonicalItemPath create(ItemPath itemPath, Class<? extends Containerable> clazz, PrismContext prismContext) {
		return new CanonicalItemPath(itemPath, clazz, prismContext);
	}

	public static CanonicalItemPath create(ItemPath itemPath) {
		return new CanonicalItemPath(itemPath, null, null);
	}

	private CanonicalItemPath(ItemPath itemPath, Class<? extends Containerable> clazz, PrismContext prismContext) {
		ItemDefinition def = clazz != null && prismContext != null ?
				prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz) : null;
		while (!ItemPath.isNullOrEmpty(itemPath)) {
			ItemPathSegment first = itemPath.first();
			if (first instanceof NameItemPathSegment) {
				// TODO what about variable named item path segments?
				QName name = ((NameItemPathSegment) first).getName();
				if (def instanceof PrismContainerDefinition) {
					def = ((PrismContainerDefinition) def).findItemDefinition(name);
					if (def != null && !QNameUtil.hasNamespace(name)) {
						name = def.getName();
					}
				}
				addToSegments(name);
			} else if (first instanceof IdItemPathSegment) {
				// ignored (for now)
			} else {
				throw new UnsupportedOperationException("Canonicalization of non-name/non-ID segments is not supported: " + first);
			}
			itemPath = itemPath.rest();
		}
	}

	private void addToSegments(QName name) {
		if (!QNameUtil.hasNamespace(name)) {
			segments.add(new Segment(name, null, null));
			return;
		}
		String namespace = name.getNamespaceURI();
		int index = 0;
		Integer shortcut = null;
		for (Segment segment : segments) {
			if (namespace.equals(segment.name.getNamespaceURI())) {
				shortcut = index = segment.index;
				break;
			}
			if (QNameUtil.hasNamespace(segment.name) && segment.shortcut == null) {
				// we found a unique non-empty namespace! (so increase the index)
				index++;
			}
		}
		segments.add(new Segment(name, index, shortcut));
	}

	public List<Segment> getSegments() {
		return segments;
	}

	public int size() {
		return segments.size();
	}

	public String asString(int howManySegments) {
		StringBuilder sb = new StringBuilder();
		Iterator<Segment> iterator = segments.iterator();
		while (iterator.hasNext() && howManySegments > 0) {
			Segment segment = iterator.next();
			howManySegments--;
			sb.append("\\");
			if (segment.shortcut == null) {		// always true for unqualified names
				sb.append(QNameUtil.qNameToUri(segment.name));
			} else {
				sb.append(SHORTCUT_MARKER).append(segment.shortcut)
						.append(QNameUtil.DEFAULT_QNAME_URI_SEPARATOR_CHAR).append(segment.name.getLocalPart());
			}
		}
		return sb.length() == 0 ? "\\" : sb.toString();			// TODO should we really return "\\" on empty path?
	}

	public String asString() {
		return asString(segments.size());
	}

	@Override
	public String toString() {
		return asString();
	}

}
