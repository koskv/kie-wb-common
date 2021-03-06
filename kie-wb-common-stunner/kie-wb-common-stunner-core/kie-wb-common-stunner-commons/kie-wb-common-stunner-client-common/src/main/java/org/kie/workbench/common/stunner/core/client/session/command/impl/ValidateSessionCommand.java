/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.client.session.command.impl;

import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;
import org.kie.workbench.common.stunner.core.client.session.impl.AbstractClientFullSession;
import org.kie.workbench.common.stunner.core.client.validation.canvas.CanvasValidationViolation;
import org.kie.workbench.common.stunner.core.client.validation.canvas.CanvasValidatorCallback;

import javax.enterprise.context.Dependent;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

@Dependent
public class ValidateSessionCommand extends AbstractClientSessionCommand<AbstractClientFullSession> {

    public ValidateSessionCommand() {
        super( true );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T> void execute( final Callback<T> callback ) {
        checkNotNull( "callback", callback );

        getSession().getCanvasValidationControl().validate( new CanvasValidatorCallback() {

            @Override
            public void onSuccess() {
                callback.onSuccess( null );
            }

            @Override
            public void onFail( final Iterable<CanvasValidationViolation> violations ) {
                    callback.onSuccess( ( T ) violations );
            }
        } );
    }

}
