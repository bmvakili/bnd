package biz.aQute.r5.resource;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.libg.version.VersionRange;
import biz.aQute.r5.resource.CapReq.MODE;
import biz.aQute.r5.resource.filters.AndFilter;
import biz.aQute.r5.resource.filters.Filter;
import biz.aQute.r5.resource.filters.SimpleFilter;

public class CapReqBuilder {

	private final String				namespace;
	private Resource					resource;
	private final Map<String,Object>	attributes	= new HashMap<String,Object>();
	private final Map<String,String>	directives	= new HashMap<String,String>();

	public CapReqBuilder(String namespace) {
		this.namespace = namespace;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	public CapReqBuilder setResource(Resource resource) {
		this.resource = resource;
		return this;
	}

	public CapReqBuilder addAttribute(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	public CapReqBuilder addDirective(String name, String value) {
		directives.put(name, value);
		return this;
	}
	
	public Capability buildCapability() {
		// TODO check the thrown exception
		if (resource == null) throw new IllegalStateException("Cannot build Capability with null Resource.");
		return new CapReq(MODE.Capability, namespace, resource, directives, attributes);
	}
	
	public Requirement buildRequirement() {
		// TODO check the thrown exception
		if (resource == null) throw new IllegalStateException("Cannot build Requirement with null Resource.");
		return new CapReq(MODE.Requirement, namespace, resource, directives, attributes);
	}

	public Requirement buildSyntheticRequirement() {
		return new CapReq(MODE.Requirement, namespace, null, directives, attributes);
	}
	
	public static final CapReqBuilder createPackageRequirement(String pkgName, VersionRange range) {
		Filter filter;
		SimpleFilter pkgNameFilter = new SimpleFilter(PackageNamespace.PACKAGE_NAMESPACE, pkgName);
		if (range != null)
			filter = new AndFilter().addChild(pkgNameFilter).addChild(Filters.fromVersionRange(range));
		else
			filter = pkgNameFilter;
		
		return new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
	}
}