package org.aggregateframework.repository;

import org.aggregateframework.context.Assert;
import org.aggregateframework.context.CollectionUtils;
import org.aggregateframework.context.DomainObjectUtils;
import org.aggregateframework.entity.AggregateRoot;
import org.aggregateframework.eventhandling.EventBus;
import org.aggregateframework.eventhandling.SimpleEventBus;
import org.aggregateframework.session.AggregateEntry;
import org.aggregateframework.session.LocalSessionFactory;
import org.aggregateframework.session.SaveAggregateCallback;
import org.aggregateframework.session.SessionFactory;
import org.aggregateframework.SystemException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: changming.xie
 * Date: 14-7-21
 * Time: 下午6:51
 */
public abstract class AbstractAggregateRepository<T extends AggregateRoot<ID>, ID extends Serializable> implements AggregateRepository<T, ID> {

    protected final Class<T> aggregateType;
    protected SessionFactory sessionFactory = LocalSessionFactory.INSTANCE;
    private EventBus eventBus = SimpleEventBus.INSTANCE;
    private SaveAggregateCallback<T> saveAggregateCallback = new SimpleSaveAggregateCallback();

    protected AbstractAggregateRepository(Class<T> aggregateType) {
        this.aggregateType = aggregateType;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public T save(T entity) {

        AggregateEntry<T> aggregateEntry = new AggregateEntry<T>(entity, saveAggregateCallback, eventBus);

        sessionFactory.requireClientSession().registerAggregate(aggregateEntry);

        return entity;
    }

    @Override
    public List<T> save(Collection<T> entities) {
        List<T> result = new ArrayList<T>();

        if (entities == null) {
            return result;
        }

        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    @Override
    public void flush() {
        sessionFactory.requireClientSession().flush();
    }

    @Override
    public T findOne(ID id) {

        T entity = null;

        entity = sessionFactory.requireClientSession().findInLocalCache(this.aggregateType, id);

        if (entity != null) {
            return entity;
        }

//        entity = sharedCacheAdapter.findOne(aggregateType, id);

        if (entity != null) {

            sessionFactory.requireClientSession().registerOriginalCopy(entity);
            sessionFactory.requireClientSession().registerToLocalCache(entity);

            return entity;
        }

        entity = doFindOne(id);

        if (entity == null) {
            return null;
        }

        entity.clearDomainEvents();

//        sharedCacheAdapter.save(this.aggregateType, entity);

        sessionFactory.requireClientSession().registerOriginalCopy(entity);
        sessionFactory.requireClientSession().registerToLocalCache(entity);

        return entity;
    }

    @Override
    public boolean exists(ID id) {

        T entity = findOne(id);

        return entity != null;
    }

    @Override
    public List<T> findAll() {
        List<ID> ids = doFindAllIds();
        return findAll(ids);
    }

    @Override
    public List<T> findAll(Collection<ID> ids) {

        List<T> entities = new ArrayList<T>();

        if (CollectionUtils.isEmpty(ids)) {
            return entities;
        }

        List<ID> idsNeedFetch = new ArrayList<ID>(ids);

        List<ID> idsInLocalCache = new ArrayList<ID>();

        List<T> entitiesNotInCache = new ArrayList<T>();


        for (ID id : idsNeedFetch) {
            T cachedEntity = sessionFactory.requireClientSession().findInLocalCache(this.aggregateType, id);

            if (cachedEntity != null) {

                entities.add(cachedEntity);
                idsInLocalCache.add(id);
            }
        }

        idsNeedFetch.removeAll(idsInLocalCache);

        if (!CollectionUtils.isEmpty(idsNeedFetch))

        {
            Collection<T> foundEntities = doFindAll(idsNeedFetch);
            entities.addAll(foundEntities);
            entitiesNotInCache.addAll(foundEntities);
        }

        for (T entity : entitiesNotInCache) {
            sessionFactory.requireClientSession().registerOriginalCopy(entity);
            sessionFactory.requireClientSession().registerToLocalCache(entity);
        }

        return entities;
    }

    @Override
    public long count() {
        return doCount();
    }

    @Override
    public void delete(ID id) {
        Assert.notNull(id, "The given id must not be null!");

        if (!exists(id)) {
            throw new SystemException("entity does not exists");
        }

        delete(findOne((ID) id));
    }

    @Override
    public void delete(T entity) {
        DomainObjectUtils.setField(entity, DomainObjectUtils.IS_DELETED, true);

        save(entity);
    }

    @Override
    public void delete(Collection<T> entities) {
        Assert.notNull(entities, "The given Iterable of entities not be null!");

        for (T entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        for (T element : findAll()) {
            delete(element);
        }
    }

    protected abstract T doSave(T entity);

    protected abstract void doDelete(T aggregate);

    protected abstract T doFindOne(ID id);

    protected abstract boolean doExists(ID id);

    protected abstract List<ID> doFindAllIds();

    protected abstract List<T> doFindAll(Collection<ID> ids);

    protected abstract long doCount();

    private class SimpleSaveAggregateCallback implements SaveAggregateCallback<T> {
        @Override
        public void save(final T aggregate) {
            if (aggregate.isDeleted()) {
                doDelete(aggregate);
            } else {
                doSave(aggregate);
            }
        }

    }

}