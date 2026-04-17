import { Redirect, Route } from 'react-router-dom';
import { IonApp, IonRouterOutlet, setupIonicReact } from '@ionic/react';
import { IonReactRouter } from '@ionic/react-router';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Products from './pages/Products';
import ProductUpsert from './pages/ProductUpsert';
import Customers from './pages/Customers';
import CustomerUpsert from './pages/CustomerUpsert';
import Orders from './pages/Orders';
import OrderCreate from './pages/OrderCreate';
import OrderDetail from './pages/OrderDetail';
import Transactions from './pages/Transactions';
import TransactionCreate from './pages/TransactionCreate';
import InventoryAdjustments from './pages/InventoryAdjustments';
import InventoryAdjustmentCreate from './pages/InventoryAdjustmentCreate';
import InventoryLogs from './pages/InventoryLogs';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import PrivateRoute from './auth/PrivateRoute';

/* Core CSS required for Ionic components to work properly */
import '@ionic/react/css/core.css';

/* Basic CSS for apps built with Ionic */
import '@ionic/react/css/normalize.css';
import '@ionic/react/css/structure.css';
import '@ionic/react/css/typography.css';

/* Optional CSS utils that can be commented out */
import '@ionic/react/css/padding.css';
import '@ionic/react/css/float-elements.css';
import '@ionic/react/css/text-alignment.css';
import '@ionic/react/css/text-transformation.css';
import '@ionic/react/css/flex-utils.css';
import '@ionic/react/css/display.css';

/**
 * Ionic Dark Mode
 * -----------------------------------------------------
 * For more info, please see:
 * https://ionicframework.com/docs/theming/dark-mode
 */

/* import '@ionic/react/css/palettes/dark.always.css'; */
/* import '@ionic/react/css/palettes/dark.class.css'; */
import '@ionic/react/css/palettes/dark.system.css';

/* Theme variables */
import './theme/variables.css';

setupIonicReact({ mode: 'md' });

const LoginRoute: React.FC = () => {
  const { status } = useAuth();
  if (status === 'authenticated') return <Redirect to="/home" />;
  return <Login />;
};

const RegisterRoute: React.FC = () => {
  const { status } = useAuth();
  if (status === 'authenticated') return <Redirect to="/home" />;
  return <Register />;
};

const App: React.FC = () => (
  <IonApp>
    <AuthProvider>
      <IonReactRouter>
        <IonRouterOutlet>
          <Route exact path="/login">
            <LoginRoute />
          </Route>

          <Route exact path="/register">
            <RegisterRoute />
          </Route>

          <PrivateRoute exact path="/home" component={Home} />

          <PrivateRoute exact path="/products" component={Products} />
          <PrivateRoute exact path="/products/new" component={ProductUpsert} />
          <PrivateRoute exact path="/products/:id/edit" component={ProductUpsert} />

          <PrivateRoute exact path="/customers" component={Customers} />
          <PrivateRoute exact path="/customers/new" component={CustomerUpsert} />
          <PrivateRoute exact path="/customers/:id/edit" component={CustomerUpsert} />

          <PrivateRoute exact path="/orders" component={Orders} />
          <PrivateRoute exact path="/orders/new" component={OrderCreate} />
          <PrivateRoute exact path="/orders/:id" component={OrderDetail} />

          <PrivateRoute exact path="/transactions" component={Transactions} />
          <PrivateRoute exact path="/transactions/new" component={TransactionCreate} />

          <PrivateRoute exact path="/inventory/adjustments" component={InventoryAdjustments} />
          <PrivateRoute exact path="/inventory/adjustments/new" component={InventoryAdjustmentCreate} />
          <PrivateRoute exact path="/inventory/logs" component={InventoryLogs} />

          <Route exact path="/">
            <Redirect to="/home" />
          </Route>
        </IonRouterOutlet>
      </IonReactRouter>
    </AuthProvider>
  </IonApp>
);

export default App;
