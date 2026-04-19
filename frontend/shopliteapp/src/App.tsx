import { Redirect, Route } from 'react-router-dom';
import { IonApp, IonRouterOutlet, setupIonicReact } from '@ionic/react';
import { IonReactRouter } from '@ionic/react-router';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Orders from './pages/Orders';
import OrderCreate from './pages/OrderCreate';
import SalesPage from './pages/SalesPage';
import DraftOrdersPage from './pages/DraftOrdersPage';
import ProductsPage from './pages/ProductsPage';
import ProductAddPage from './pages/ProductAddPage';
import ProductDetailPage from './pages/ProductDetailPage';
import ProductEditPage from './pages/ProductEditPage';
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

          <PrivateRoute exact path="/products" component={ProductsPage} />
          <PrivateRoute exact path="/product/new" component={ProductAddPage} />
          <PrivateRoute exact path="/products/:id" component={ProductDetailPage} />
          <PrivateRoute exact path="/products/:id/edit" component={ProductEditPage} />
          
          <PrivateRoute exact path="/orders" component={Orders} />
          <PrivateRoute exact path="/orders/draft" component={DraftOrdersPage} />
          <PrivateRoute exact path="/orders/new" component={OrderCreate} />

          <PrivateRoute exact path="/sales" component={SalesPage} />

          <Route exact path="/">
            <Redirect to="/login" />
          </Route>
        </IonRouterOutlet>
      </IonReactRouter>
    </AuthProvider>
  </IonApp>
);

export default App;
