/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.library.client.screens;

import org.guvnor.common.services.project.model.Project;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.kie.workbench.common.screens.library.api.LibraryContextSwitchEvent;
import org.kie.workbench.common.screens.library.api.LibraryInfo;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.events.ProjectDetailEvent;
import org.kie.workbench.common.screens.library.client.perspective.LibraryPerspective;
import org.kie.workbench.common.screens.library.client.resources.i18n.LibraryConstants;
import org.kie.workbench.common.screens.library.client.util.LibraryBreadcrumbs;
import org.kie.workbench.common.screens.library.client.util.LibraryDocks;
import org.kie.workbench.common.screens.library.client.util.LibraryParameters;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.kie.workbench.common.screens.library.client.widgets.LibraryBreadCrumbToolbarPresenter;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.security.ResourceRef;
import org.uberfire.security.authz.AuthorizationManager;
import org.uberfire.workbench.model.ActivityResourceType;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@WorkbenchScreen( identifier = LibraryPlaces.LIBRARY_SCREEN,
        owningPerspective = LibraryPerspective.class )
public class LibraryScreen {

    public interface View extends UberElement<LibraryScreen> {

        void clearProjects();

        void addProject( String project, Command details, Command select );

        void clearFilterText();
    }

    private View view;

    private LibraryBreadCrumbToolbarPresenter breadCrumbToolbarPresenter;

    private LibraryDocks libraryDocks;

    private PlaceManager placeManager;

    private LibraryBreadcrumbs libraryBreadcrumbs;

    private Event<LibraryContextSwitchEvent> libraryContextSwitchEvent;

    private SessionInfo sessionInfo;

    private AuthorizationManager authorizationManager;

    private TranslationService ts;

    private Event<ProjectDetailEvent> projectDetailEvent;

    Caller<LibraryService> libraryService;

    LibraryInfo libraryInfo;

    @Inject
    public LibraryScreen( final View view,
                          final LibraryBreadCrumbToolbarPresenter breadCrumbToolbarPresenter,
                          final LibraryDocks libraryDocks,
                          final PlaceManager placeManager,
                          final LibraryBreadcrumbs libraryBreadcrumbs,
                          final Event<LibraryContextSwitchEvent> libraryContextSwitchEvent,
                          final SessionInfo sessionInfo,
                          final AuthorizationManager authorizationManager,
                          final TranslationService ts,
                          final Event<ProjectDetailEvent> projectDetailEvent,
                          final Caller<LibraryService> libraryService ) {
        this.view = view;
        this.breadCrumbToolbarPresenter = breadCrumbToolbarPresenter;
        this.libraryDocks = libraryDocks;
        this.placeManager = placeManager;
        this.libraryBreadcrumbs = libraryBreadcrumbs;
        this.libraryContextSwitchEvent = libraryContextSwitchEvent;
        this.sessionInfo = sessionInfo;
        this.authorizationManager = authorizationManager;
        this.ts = ts;
        this.projectDetailEvent = projectDetailEvent;
        this.libraryService = libraryService;
    }

    @OnStartup
    public void onStartup( final PlaceRequest place ) {
        loadDefaultLibrary();
    }

    private void loadDefaultLibrary() {
        libraryService.call( new RemoteCallback<LibraryInfo>() {
            @Override
            public void callback( LibraryInfo libraryInfo ) {
                if ( libraryInfo.isFullLibrary() ) {
                    loadLibrary( libraryInfo );
                } else {
                    placeManager.goTo( LibraryPlaces.NEW_PROJECT_PERSPECTIVE );
                }
            }
        } ).getDefaultLibraryInfo();

        setupToolBar();
    }

    private void loadLibrary( LibraryInfo libraryInfo ) {
        LibraryScreen.this.libraryInfo = libraryInfo;
        setupProjects( libraryInfo.getProjects() );
        setupOus( libraryInfo );
        libraryDocks.refresh();
    }

    private void setupToolBar() {
        libraryBreadcrumbs.setupToolBar( breadCrumbToolbarPresenter );
    }

    private void setupOus( LibraryInfo libraryInfo ) {
        breadCrumbToolbarPresenter.init( ou -> {
            selectOrganizationUnit( ou );
        }, libraryInfo );
    }

    private void updateLibrary( String ou ) {
        libraryService.call( new RemoteCallback<LibraryInfo>() {
            @Override
            public void callback( LibraryInfo libraryInfo ) {
                LibraryScreen.this.libraryInfo = libraryInfo;
                view.clearFilterText();
                setupProjects( libraryInfo.getProjects() );
            }
        } ).getLibraryInfo( ou );
    }

    private void setupProjects( Set<Project> projects ) {
        view.clearProjects();

        projects.stream().forEach( p -> view
                .addProject( p.getProjectName(), detailsCommand( p ),
                             selectCommand( p ) ) );
    }

    public void newProject() {
        libraryDocks.hide();
        placeManager.goTo( new DefaultPlaceRequest( LibraryPlaces.NEW_PROJECT_SCREEN, newProjectParameters() ) );
    }

    private Map<String, String> newProjectParameters() {
        Map<String, String> param = new HashMap<>();
        param.put( LibraryParameters.BACK_PLACE, LibraryPlaces.LIBRARY_SCREEN );
        param.put( LibraryParameters.SELECTED_OU, libraryInfo.getSelectedOrganizationUnit().getIdentifier() );
        return param;
    }


    Command selectCommand( Project project ) {
        return () -> {
            placeManager.goTo( LibraryPlaces.PROJECT_SCREEN );
            projectDetailEvent.fire( new ProjectDetailEvent( project ) );

            detailsCommand( project ).execute();
        };
    }

    boolean hasAccessToPerspective( String perspectiveId ) {
        ResourceRef resourceRef = new ResourceRef( perspectiveId, ActivityResourceType.PERSPECTIVE );
        return authorizationManager.authorize( resourceRef, sessionInfo.getIdentity() );
    }

    Command detailsCommand( Project selectedProject ) {
        return () -> {
            libraryDocks.handle( selectedProject );
        };
    }

    public void selectOrganizationUnit( String ou ) {
        updateLibrary( ou );
    }

    public void updateProjectsBy( String filter ) {
        if ( libraryInfo != null && libraryInfo.isFullLibrary() ) {
            Set<Project> filteredProjects = filterProjects( filter );

            setupProjects( filteredProjects );
        }
    }

    Set<Project> filterProjects( String filter ) {
        return libraryInfo.getProjects().stream()
                .filter( p -> p.getProjectName() != null )
                .filter( p -> p.getProjectName().toUpperCase()
                        .startsWith( filter.toUpperCase() ) )
                .collect( Collectors.toSet() );
    }

    public void importExample() {
        if ( hasAccessToPerspective( LibraryPlaces.AUTHORING ) ) {

            libraryBreadcrumbs.setupAuthoringBreadcrumbsForExample();

            placeManager.goTo( new DefaultPlaceRequest( LibraryPlaces.AUTHORING ) );
            libraryContextSwitchEvent
                    .fire( new LibraryContextSwitchEvent( LibraryContextSwitchEvent.EventType.PROJECT_FROM_EXAMPLE ) );
        }
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return ts.getTranslation( LibraryConstants.LibraryScreen );
    }

    @WorkbenchPartView
    public UberElement<LibraryScreen> getView() {
        return view;
    }
}
