import React from 'react';
import { Redirect, Route } from 'react-router-dom';
import { IonApp, IonRouterOutlet, setupIonicReact } from '@ionic/react';
import { IonReactRouter } from '@ionic/react-router';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Orders from './pages/Orders';
import OrderCreate from './pages/OrderCreate';
import OrderDetailPage from './pages/OrderDetailPage';
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
import InventoryAdjustmentsPage from './pages/InventoryAdjustmentsPage';
import InventoryAdjustmentCreatePage from './pages/InventoryAdjustmentCreatePage';
import InventoryAdjustmentDetailPage from './pages/InventoryAdjustmentDetailPage';
import ImportReturnOrdersPage from './pages/ImportReturnOrdersPage';
import ImportReturnCreatePage from './pages/ImportReturnCreatePage';
import ImportReturnOrderDetailPage from './pages/ImportReturnOrderDetailPage';
import MorePage from './pages/MorePage';
import FundLedgerPage from './pages/FundLedgerPage';
import WelcomePage from './pages/WelcomePage';
import GetStartedPage from './pages/GetStartedPage';
import OtpPage from './pages/OtpPage';
import SetStorePage from './pages/SetStorePage';
import SetPasswordPage from './pages/SetPasswordPage';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import RequireAuth from './auth/RequireAuth';
import { usePushNotifications } from './utils/usePushNotifications';

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

/**
 * Inner component — lives inside IonReactRouter so useIonRouter() works.
 * Also the right place for the push notification hook (needs hook + router context).
 */
const AppContent: React.FC = () => {
  usePushNotifications();

  return (
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
      <Route exact path="/orders/:id(\d+)" render={() => <RequireAuth component={OrderDetailPage} />} />

      <Route exact path="/sales" render={() => <RequireAuth component={SalesPage} />} />
      <Route exact path="/employees" render={() => <RequireAuth component={EmployeesPage} />} />
      <Route exact path="/roles/:id" render={() => <RequireAuth component={RoleDetailPage} />} />

      <Route exact path="/import-orders" render={() => <RequireAuth component={ImportOrdersPage} />} />
      <Route exact path="/import-order/new" render={() => <RequireAuth component={ImportOrderCreatePage} />} />
      <Route exact path="/import-order/edit/:id" render={() => <RequireAuth component={ImportOrderCreatePage} />} />
      <Route exact path="/import-orders/:id" render={() => <RequireAuth component={ImportOrderDetailPage} />} />

      <Route exact path="/inventory-adjustments" render={() => <RequireAuth component={InventoryAdjustmentsPage} />} />
      <Route exact path="/inventory-adjustment/new" render={() => <RequireAuth component={InventoryAdjustmentCreatePage} />} />
      <Route exact path="/inventory-adjustments/:id" render={() => <RequireAuth component={InventoryAdjustmentDetailPage} />} />

      <Route exact path="/import-return-orders" render={() => <RequireAuth component={ImportReturnOrdersPage} />} />
      <Route exact path="/import-return-orders/create/:importOrderId" render={() => <RequireAuth component={ImportReturnCreatePage} />} />
      <Route exact path="/import-return-orders/:id" render={() => <RequireAuth component={ImportReturnOrderDetailPage} />} />

      <Route exact path="/fund-ledger" render={() => <RequireAuth component={FundLedgerPage} />} />

      <Route exact path="/more" render={() => <RequireAuth component={MorePage} />} />

      <Route exact path="/welcome" render={() => <WelcomePage />} />
      <Route exact path="/get-started" render={() => <GetStartedPage />} />
      <Route exact path="/otp" render={() => <OtpPage />} />
      <Route exact path="/register/store" render={() => <SetStorePage />} />
      <Route exact path="/register/complete" render={() => <SetPasswordPage />} />

      <Route exact path="/">
        <Redirect to="/welcome" />
      </Route>
    </IonRouterOutlet>
  );
};

const App: React.FC = () => (
  <IonApp>
    <AuthProvider>
      <IonReactRouter>
        <AppContent />
      </IonReactRouter>
    </AuthProvider>
  </IonApp>
);

export default App;
