/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.definition.adapter;

import org.kie.workbench.common.stunner.core.api.FactoryManager;
import org.kie.workbench.common.stunner.core.definition.adapter.binding.BindableAdapterUtils;
import org.kie.workbench.common.stunner.core.definition.adapter.binding.HasInheritance;
import org.kie.workbench.common.stunner.core.definition.morph.MorphDefinition;
import org.kie.workbench.common.stunner.core.definition.morph.MorphProperty;
import org.kie.workbench.common.stunner.core.definition.util.DefinitionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class BindableMorphAdapter<S> extends AbstractMorphAdapter<S> {

    public BindableMorphAdapter( DefinitionUtils definitionUtils,
                                 FactoryManager factoryManager ) {
        super( definitionUtils, factoryManager );

    }

    protected abstract <T> T doMerge( S source, T result );

    @Override
    @SuppressWarnings( "unchecked" )
    protected <T> T doMerge( final S source,
                             final MorphDefinition morphDefinition,
                             final T result ) {
        if ( definitionUtils.isNonePolicy( morphDefinition ) ) {
            return result;

        }
        if ( definitionUtils.isDefaultPolicy( morphDefinition ) ) {
            final Object nameProperty = getDefinitionManager().adapters().forDefinition().getNameProperty( source );
            final Object namePropertyValue = getDefinitionManager().adapters().forProperty().getValue( nameProperty );
            final Object targetNameProperty = getDefinitionManager().adapters().forDefinition().getNameProperty( result );
            getDefinitionManager().adapters().forProperty().setValue( targetNameProperty, namePropertyValue );
            ;
            return result;

        }
        return doMerge( source, result );

    }

    @Override
    public <T> Iterable<MorphProperty> getMorphProperties( final T definition ) {
        return getMorphPropertiesForType( definition.getClass() );
    }

    @Override
    public <T> Iterable<MorphDefinition> getMorphDefinitions( final T definition ) {
        return getMorphDefinitionsForType( definition.getClass() );
    }

    public Iterable<MorphDefinition> getMorphDefinitionsForType( final Class<?> type ) {
        final String dId = getDefinitionId( type );
        final String baseId = getBaseDefinitionId( type );
        return super.getMorphDefinitions( dId, baseId );
    }

    public <T> Iterable<MorphProperty> getMorphPropertiesForType( final Class<?> type ) {
        final String dId = getDefinitionId( type );
        final String baseId = getBaseDefinitionId( type );
        return super.getMorphProperties( dId, baseId );
    }

    public <T> Iterable<String> getTargetsForType( final Class<?> type ) {
        final String dId = getDefinitionId( type );
        final String baseId = getBaseDefinitionId( type );
        return getTargets( type, dId, baseId );

    }

    @Override
    public boolean isPojoModel() {
        return true;
    }

    @Override
    protected Iterable<String> getTargets( final Class<?> type,
                                           final String definitionId,
                                           final String baseId ) {
        final Iterable<String> superTargets = super.getTargets( type, definitionId, baseId );
        if ( null != superTargets && superTargets.iterator().hasNext() ) {
            final Set<String> result = new LinkedHashSet<>();
            for ( final String s : superTargets ) {
                final String[] types = getTypes( type, s );
                if ( null != types && types.length > 0 ) {
                    Collections.addAll( result, types );

                } else {
                    result.add( s );

                }

            }
            return result;

        }
        return null;
    }

    protected String[] getTypes( final Class<?> adapterType, String baseType ) {
        final DefinitionAdapter<Object> definitionAdapter = getDefinitionManager().adapters().registry().getDefinitionAdapter( adapterType );
        if ( definitionAdapter instanceof HasInheritance ) {
            return ( ( HasInheritance ) definitionAdapter ).getTypes( baseType );
        }
        return null;

    }

    @SuppressWarnings( "unchecked" )
    protected String getBaseDefinitionId( final Class<?> type ) {
        final DefinitionAdapter<Object> definitionAdapter = getDefinitionManager().adapters().registry().getDefinitionAdapter( type );
        if ( definitionAdapter instanceof HasInheritance ) {
            return ( ( HasInheritance ) definitionAdapter ).getBaseType( type );
        }
        return null;
    }

    protected String getDefinitionId( final Class<?> type ) {
        return BindableAdapterUtils.getDefinitionId( type );
    }

}
