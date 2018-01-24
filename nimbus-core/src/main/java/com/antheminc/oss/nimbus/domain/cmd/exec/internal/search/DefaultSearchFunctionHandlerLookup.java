/**
 *  Copyright 2016-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.antheminc.oss.nimbus.domain.cmd.exec.internal.search;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;

import com.anthem.oss.nimbus.core.domain.model.config.ParamValue;
import com.antheminc.oss.nimbus.FrameworkRuntimeException;
import com.antheminc.oss.nimbus.domain.cmd.Command;
import com.antheminc.oss.nimbus.domain.cmd.CommandElement.Type;
import com.antheminc.oss.nimbus.domain.cmd.exec.CommandPathVariableResolver;
import com.antheminc.oss.nimbus.domain.cmd.exec.ExecutionContext;
import com.antheminc.oss.nimbus.domain.defn.Repo;
import com.antheminc.oss.nimbus.domain.model.config.ModelConfig;
import com.antheminc.oss.nimbus.domain.model.state.EntityState.Param;
import com.antheminc.oss.nimbus.domain.model.state.repo.ModelRepository;
import com.antheminc.oss.nimbus.entity.StaticCodeValue;
import com.antheminc.oss.nimbus.entity.SearchCriteria.LookupSearchCriteria;
import com.antheminc.oss.nimbus.entity.SearchCriteria.ProjectCriteria;

/**
 * @author Rakesh Patel
 *
 */
@SuppressWarnings("unchecked")
public class DefaultSearchFunctionHandlerLookup<T, R> extends DefaultSearchFunctionHandler<T, R> {

	@Override
	public R execute(ExecutionContext executionContext, Param<T> actionParameter) {
		
		ModelConfig<?> mConfig = getRootDomainConfig(executionContext);
		
		LookupSearchCriteria lookupSearchCriteria = createSearchCriteria(executionContext, mConfig, actionParameter);
		Class<?> criteriaClass = mConfig.getReferredClass();
		String alias = findRepoAlias(mConfig);
		
		ModelRepository rep = getRepFactory().get(mConfig.getRepo()); //TODO what if it is a non db search like WS call for search ? 
		
		List<?> searchResult = (List<?>)rep._search(criteriaClass, alias, lookupSearchCriteria);

		Command cmd = executionContext.getCommandMessage().getCommand();
		if(StringUtils.equalsIgnoreCase(cmd.getElement(Type.DomainAlias).get().getAlias(), "staticCodeValue")) {
			return getStaticParamValues((List<StaticCodeValue>)searchResult, cmd);
		}
		return getDynamicParamValues(lookupSearchCriteria, criteriaClass, searchResult);
	}

	@Override
	protected LookupSearchCriteria createSearchCriteria(ExecutionContext executionContext, ModelConfig<?> mConfig, Param<T> actionParameter) {
		Command cmd = executionContext.getCommandMessage().getCommand();
		
		LookupSearchCriteria lookupSearchCriteria = new LookupSearchCriteria();
		lookupSearchCriteria.validate(executionContext);
			
		resolveNamedQueryIfApplicable(executionContext, mConfig, lookupSearchCriteria, actionParameter);
		
		ProjectCriteria projectCriteria = new ProjectCriteria();
		
		if(cmd.getRequestParams().get("projection.alias") != null) {
			projectCriteria.setAlias(cmd.getFirstParameterValue("projection.alias"));
		}
		else if(cmd.getRequestParams().get("projection.mapsTo") != null) {
			String projectMapping = cmd.getFirstParameterValue("projection.mapsTo");
			String[] keyValues = StringUtils.split(projectMapping,",");
			
			Stream.of(keyValues).forEach((kvString) -> {
				if(MapUtils.isEmpty(projectCriteria.getMapsTo())){
					projectCriteria.setMapsTo(new HashMap<String, String>());
				}
				String[] kv = StringUtils.split(kvString,":");
				projectCriteria.getMapsTo().put(kv[0], kv[1]);
			});
		}
		
		lookupSearchCriteria.setProjectCriteria(projectCriteria);
		
		return lookupSearchCriteria;
	}
	
	private R getStaticParamValues(List<StaticCodeValue> searchResult, Command cmd) {	
		if(CollectionUtils.isEmpty(searchResult))
			return null;
		
		if(CollectionUtils.size(searchResult) > 1)
			throw new IllegalStateException("StaticCodeValue search for a command "+cmd+" returned more than one records for paramCode");
		
		return (R) searchResult.get(0).getParamValues();
	}
	
	private R getDynamicParamValues(LookupSearchCriteria lookupSearchCriteria, Class<?> criteriaClass, List<?> searchResult) {
		List<String> list = new ArrayList<String>(lookupSearchCriteria.getProjectCriteria().getMapsTo().values());
		
		if(list.size() > 2)
			throw new IllegalStateException("ParamValues lookup failed due to more than 2 fields provided to create the param values. the criteria class is "+criteriaClass);
		
		PropertyDescriptor codePd = BeanUtils.getPropertyDescriptor(criteriaClass, list.get(0));
		PropertyDescriptor labelPd = BeanUtils.getPropertyDescriptor(criteriaClass, list.get(1));
		
		try {
			List<ParamValue> paramValues = new ArrayList<>();
			for(Object model: searchResult) {
				paramValues.add(new ParamValue((String)codePd.getReadMethod().invoke(model), (String)labelPd.getReadMethod().invoke(model)));
			}
			return (R)paramValues;
		}
		catch(Exception ex) {
			throw new FrameworkRuntimeException("Failed to execute read on property: "+codePd+" and "+labelPd, ex);
		}
	}
	
	private void resolveNamedQueryIfApplicable(ExecutionContext executionContext, ModelConfig<?> mConfig, LookupSearchCriteria lookupSearchCriteria, Param<T> actionParameter) {
		String where = executionContext.getCommandMessage().getCommand().getFirstParameterValue("where");
		
		// find if where is a named query
		Repo repo = mConfig.getRepo();
		Repo.NamedNativeQuery[] namedQueries = repo.namedNativeQueries();
		
		if(namedQueries != null && namedQueries.length > 0) {
			for(Repo.NamedNativeQuery query: namedQueries) {
				if(StringUtils.equalsIgnoreCase(query.name(), where) && query.nativeQueries() != null) {
					int i = 0;
					for(String q: query.nativeQueries()) {
						if(i == 0) {
							where = q;
						}
						else {
							where = where +"~~"+q;
						}
						i++;
					}
				}
			}
		}
		//resolve path variables if any
		where = getPathVariableResolver().resolve(actionParameter, where);
		
		lookupSearchCriteria.setWhere(where);
	}
	
}