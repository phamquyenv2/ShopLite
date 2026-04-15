import {
  IonButton,
  IonContent,
  IonHeader,
  IonItem,
  IonList,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react';
import './Home.css';
import { useAuth } from '../auth/useAuth';

const Home: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>ShopLite</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent fullscreen>
        <IonHeader collapse="condense">
          <IonToolbar>
            <IonTitle size="large">ShopLite</IonTitle>
          </IonToolbar>
        </IonHeader>

        <div className="ion-padding">
          <IonText color="medium">
            Signed in as <strong>{user?.username ?? 'User'}</strong>
          </IonText>
        </div>

        <IonList inset>
          <IonItem routerLink="/products" button>
            Products
          </IonItem>
          <IonItem routerLink="/customers" button>
            Customers
          </IonItem>
          <IonItem routerLink="/orders" button>
            Orders
          </IonItem>
          <IonItem routerLink="/transactions" button>
            Transactions
          </IonItem>
          <IonItem routerLink="/inventory/adjustments" button>
            Inventory Adjustments
          </IonItem>
          <IonItem routerLink="/inventory/logs" button>
            Inventory Logs
          </IonItem>
        </IonList>

        <div className="ion-padding">
          <IonButton expand="block" color="medium" onClick={() => void logout()}>
            Sign out
          </IonButton>
        </div>
      </IonContent>
    </IonPage>
  );
};

export default Home;
