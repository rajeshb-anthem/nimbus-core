/**
 * 
 */
package com.anthem.oss.nimbus.core.domain.model.state.repo.db;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.anthem.oss.nimbus.core.domain.command.Action;
import com.anthem.oss.nimbus.core.domain.definition.InvalidConfigException;
import com.anthem.oss.nimbus.core.domain.definition.Repo;
import com.anthem.oss.nimbus.core.domain.model.state.EntityState;
import com.anthem.oss.nimbus.core.domain.model.state.EntityState.Param;
import com.anthem.oss.nimbus.core.domain.model.state.ModelEvent;
import com.anthem.oss.nimbus.core.domain.model.state.internal.AbstractEvent.PersistenceMode;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Soham Chakravarti
 * @author Rakesh Patel
 */
@EnableConfigurationProperties
@ConfigurationProperties(exceptionIfInvalid=true,prefix="model.persistence.strategy")
public class ParamStateAtomicPersistenceEventListener extends ParamStatePersistenceEventListener {

	ModelRepositoryFactory repoFactory;

	@Getter @Setter
	private PersistenceMode mode;
	
	ModelPersistenceHandler handler;

	public ParamStateAtomicPersistenceEventListener(ModelRepositoryFactory repoFactory, ModelPersistenceHandler handler) {
		this.repoFactory = repoFactory;
		this.handler = handler;
	}
	
	@Override
	public boolean shouldAllow(EntityState<?> p) {
		return super.shouldAllow(p) && PersistenceMode.ATOMIC == mode;
	}
	
	@Override
	public boolean listen(ModelEvent<Param<?>> event) {
		List<ModelEvent<Param<?>>> events = new ArrayList<>();
		
		ModelEvent<Param<?>> rootModelEvent = new ModelEvent<>(Action.getByName(event.getType()), event.getPayload().getRootDomain().getPath(), event.getPayload().getRootDomain().getAssociatedParam());
		
		events.add(rootModelEvent);
			
		Param<?> p = (Param<?>) event.getPayload();
		Repo repo = p.getRootDomain().getConfig().getRepo();
		if(repo==null) {
			throw new InvalidConfigException("Core Persistent entity must be configured with "+Repo.class.getSimpleName()+" annotation. Not found for root model: "+p.getRootExecution());
		} 
			
		ModelPersistenceHandler handler = repoFactory.getHandler(repo);
		
		if(handler == null) {
			throw new InvalidConfigException("There is no repository handler provided for the configured repository :"+repo.value().name()+ " for root model: "+p.getRootExecution());
		}
		
		return handler.handle(events);
		
	}

}
