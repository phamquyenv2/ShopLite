import React from 'react';
import { Redirect, Route } from 'react-router-dom';
import { IonContent, IonLoading, IonPage } from '@ionic/react';
import { useAuth } from './useAuth';

type Props = {
    path: string;
    exact?: boolean;
    component: React.ComponentType;
};

const PrivateRoute: React.FC<Props> = ({ component: Component, ...rest }) => {
    const { status } = useAuth();

    return (
        <Route
            {...rest}
            render={(props) => {
                if (status === 'checking') {
                    return (
                        <IonPage>
                            <IonContent>
                                <IonLoading isOpen message="Checking session..." />
                            </IonContent>
                        </IonPage>
                    );
                }

                if (status === 'unauthenticated') {
                    return (
                        <Redirect
                            to={{
                                pathname: '/login',
                                state: { from: props.location },
                            }}
                        />
                    );
                }

                return <Component />;
            }}
        />
    );
};

export default PrivateRoute;
