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
import EmployeesPage from './pages/EmployeesPage';
import RoleDetailPage from './pages/RoleDetailPage/RoleDetailPage';
import ImportOrdersPage from './pages/ImportOrdersPage';
import ImportOrderCreatePage from './pages/ImportOrderCreatePage';
import ImportOrderDetailPage from './pages/ImportOrderDetailPage';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import RequireAuth from './auth/RequireAuth';

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
          <Route exact path="/login" render={() => <LoginRoute />} />

          <Route exact path="/register" render={() => <RegisterRoute />} />

          <Route exact path="/home" render={() => <RequireAuth component={Home} />} />

          <Route exact path="/products" render={() => <RequireAuth component={ProductsPage} />} />
          <Route exact path="/product/new" render={() => <RequireAuth component={ProductAddPage} />} />
          <Route exact path="/products/:id" render={() => <RequireAuth component={ProductDetailPage} />} />
          <Route exact path="/products/:id/edit" render={() => <RequireAuth component={ProductEditPage} />} />
          
          <Route exact path="/orders" render={() => <RequireAuth component={Orders} />} />
          <Route exact path="/orders/draft" render={() => <RequireAuth component={DraftOrdersPage} />} />
          <Route exact path="/orders/new" render={() => <RequireAuth component={OrderCreate} />} />

          <Route exact path="/sales" render={() => <RequireAuth component={SalesPage} />} />
          <Route exact path="/employees" render={() => <RequireAuth component={EmployeesPage} />} />
          <Route exact path="/roles/:id" render={() => <RequireAuth component={RoleDetailPage} />} />

          <Route exact path="/import-orders" render={() => <RequireAuth component={ImportOrdersPage} />} />
          <Route exact path="/import-order/new" render={() => <RequireAuth component={ImportOrderCreatePage} />} />
          <Route exact path="/import-order/edit/:id" render={() => <RequireAuth component={ImportOrderCreatePage} />} />
          <Route exact path="/import-orders/:id" render={() => <RequireAuth component={ImportOrderDetailPage} />} />

          <Route exact path="/">
            <Redirect to="/login" />
          </Route>
        </IonRouterOutlet>
      </IonReactRouter>
    </AuthProvider>
  </IonApp>
);

export default App;
