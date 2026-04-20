import React from 'react';
import { Redirect } from 'react-router-dom';
import { IonContent, IonLoading, IonPage } from '@ionic/react';
import { useAuth } from './useAuth';

type Props = {
    component: React.ComponentType;
};

const RequireAuth: React.FC<Props> = ({ component: Component }) => {
    const { status } = useAuth();

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
        return <Redirect to="/login" />;
    }

    return <Component />;
};

export default RequireAuth;
