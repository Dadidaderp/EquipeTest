/**
 *     This file is part of the Squashtest platform.
 *     Copyright (C) 2010 - 2016 Henix, henix.fr
 *
 *     See the NOTICE file distributed with this work for additional
 *     information regarding copyright ownership.
 *
 *     This is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     this software is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.squashtest.tm.web.internal.interceptor

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerInterceptor;

import spock.lang.Specification
import spock.lang.Unroll;

/**
 * @author Gregory Fouquet
 *
 */
class ExcludeRequestInterceptorWrapperTest extends Specification {
	private HandlerInterceptor delegate = Mock()
	private ExcludeRequestInterceptorWrapper wrapper = new ExcludeRequestInterceptorWrapper(delegate) 

	@Unroll
	def "Request #pathInfo should match filter #extensions"() {
		given: 
		wrapper.excludedExtensions = extensions
		
		and: 
		HttpServletRequest req = Mock()
		req.pathInfo >> pathInfo
		
		when: 
		wrapper.preHandle(req, null, null)
		
		then:
		0 * delegate.preHandle(req, _, _)
		
		where:
		pathInfo | extensions
		"/some.path.foo" | ["foo", "bar"]
		"/some.path.FOO" | ["foo", "bar"]
		"/some.path.foo" | ["FOO", "bar"]
		"/some.path.foo" | [" foo ", "bar"]
	}

	@Unroll
	def "Request #pathInfo should not match filter #extensions"() {
		given: 
		wrapper.excludedExtensions = extensions
		
		and: 
		HttpServletRequest req = Mock()
		req.pathInfo >> pathInfo
		
		when: 
		wrapper.preHandle(req, null, null)
		
		then:
		1 * delegate.preHandle(req, _, _)
		
		where:
		pathInfo | extensions
		"/some.path.foo"  | []
		"/some.path.foo." | ["foo", "bar"]
		"/"               | ["foo", "bar"]
		""                | ["foo", "bar"]
		"."               | ["foo", "bar"]
		"foo"             | ["foo", "bar"]
	}
}
