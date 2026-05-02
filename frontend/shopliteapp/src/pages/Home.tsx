import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonToast,
  IonToolbar,
} from '@ionic/react';
import {
  personCircleOutline,
  cashOutline,
  cartOutline,
  cubeOutline,
  peopleOutline,
  logInOutline,
  homeOutline,
  gridOutline,
  storefrontOutline,
  receiptOutline,
  reorderThreeOutline,
  callOutline,
  notificationsOutline,
  mailOutline,
  storefront
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import { useState } from 'react';
import { useIonViewWillEnter } from '@ionic/react';
import { authApis, endpoints } from '../utils/Apis';
import { notificationService } from '../services/notification.service';
import { storeInvitationService } from '../services/storeInvitation.service';
import { hasPermission } from '../utils/permissions';
import type { Notification, Order, Permission } from '../api/types';
import type { MeResponse } from '../auth/types';
import './Home.css';

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const Home: React.FC = () => {
  const history = useHistory();

  const [todayRevenue, setTodayRevenue] = useState(0);
  const [todayOrdersCount, setTodayOrdersCount] = useState(0);
  const [todayProfit, setTodayProfit] = useState(0);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [storeName, setStoreName] = useState('');
  const [memberRole, setMemberRole] = useState('');
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const unreadCount = notifications.filter(n => !n.read).length;

  const loadNotifications = async (): Promise<Notification[]> => {
    try {
      const items = await notificationService.getNotifications();
      setNotifications(items);
      return items;
    } catch (err) {
      console.error(err);
      return [];
    }
  };

  const toggleNotifications = async () => {
    const nextOpen = !notificationOpen;
    setNotificationOpen(nextOpen);
    if (!nextOpen) return;

    const items = await loadNotifications();
    const unread = items.filter(n => !n.read);
    if (unread.length > 0) {
      void Promise.all(unread.map(n => notificationService.markRead(n.id)))
        .then(loadNotifications)
        .catch(console.error);
    }
  };

  const handleAcceptInvitation = async (notification: Notification) => {
    if (!notification.referenceId) return;
    try {
      const result = await storeInvitationService.accept(notification.referenceId);
      setStoreName(result.currentStore?.name || 'ShopLite');
      setMemberRole(result.currentStore?.memberRole || '');
      setPermissions(result.permissions || result.currentStore?.permissions || []);
      setToast('Da tham gia cua hang');
      await loadNotifications();
      history.replace('/home');
    } catch (err: any) {
      setToast(err.message || 'Khong the chap nhan loi moi');
    }
  };

  const handleDeclineInvitation = async (notification: Notification) => {
    if (!notification.referenceId) return;
    try {
      await storeInvitationService.decline(notification.referenceId);
      setToast('Da tu choi loi moi');
      await loadNotifications();
    } catch (err: any) {
      setToast(err.message || 'Khong the tu choi loi moi');
    }
  };

  useIonViewWillEnter(() => {
    const loadData = async () => {
      try {
        const meRes = await authApis().get<any>(endpoints.me);
        const mePayload = (meRes.data?.data ?? meRes.data) as MeResponse;
        const currentStore = mePayload?.currentStore ?? null;
        setStoreName(currentStore?.name || 'Chua co cua hang');
        setMemberRole(currentStore?.memberRole || '');
        setPermissions((currentStore?.permissions || []) as Permission[]);

        const now = new Date();
        const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const params = new URLSearchParams();
        params.set('from', startOfDay.toISOString());
        params.set('to', now.toISOString());

        const res = await authApis().get<any>(`${endpoints.orders}?${params.toString()}`);

        let orders: Order[] = [];
        if (Array.isArray(res.data?.data)) orders = res.data.data;
        else if (Array.isArray(res.data)) orders = res.data;

        const validOrders = orders.filter(o => {
          const isStatusValid = o.status === 'COMPLETED' || o.status === 'PENDING_PAYMENT';
          if (!isStatusValid || !o.createdAt) return false;
          const orderDate = new Date(o.createdAt);
          return orderDate >= startOfDay && orderDate <= now;
        });

        const todayOrdersForRecent = orders.filter(o => {
          if (!o.createdAt) return false;
          const orderDate = new Date(o.createdAt);
          return orderDate >= startOfDay && orderDate <= now;
        });

        setTodayRevenue(validOrders.reduce((sum, o) => sum + (o.totalAmount ?? 0), 0));
        setTodayOrdersCount(validOrders.length);
        setTodayProfit(validOrders.reduce((sum, o) => sum + ((o as any).profit ?? ((o.totalAmount ?? 0) * 0.3)), 0));

        const sorted = [...todayOrdersForRecent].sort((a, b) =>
          new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
        setRecentOrders(sorted.slice(0, 3));
      } catch (err) {
        console.error(err);
      }
    };
    loadData();
    loadNotifications();
  });

  const shortcuts = [
    { label: 'Tạo đơn', icon: cartOutline, color: 'blue', route: '/sales', show: hasPermission(permissions, '/api/v1/orders', 'POST') },
    { label: 'Sản phẩm', icon: cubeOutline, color: 'indigo', route: '/products', show: hasPermission(permissions, '/api/v1/products', 'GET') },
    { label: 'Nhân viên', icon: peopleOutline, color: 'orange', route: '/employees', show: hasPermission(permissions, '/api/v1/employees', 'GET') },
    { label: 'Nhập hàng', icon: logInOutline, color: 'red', route: '/import-orders', show: hasPermission(permissions, '/api/v1/import-orders', 'GET') },
  ].filter(item => item.show);

  const tabs = [
    { label: 'Tổng quan', icon: homeOutline, active: true, route: '/home', show: true },
    { label: 'Hàng hóa', icon: gridOutline, route: '/products', show: hasPermission(permissions, '/api/v1/products', 'GET') },
    { label: 'Bán hàng', icon: storefrontOutline, route: '/sales', show: hasPermission(permissions, '/api/v1/orders', 'POST') },
    { label: 'Hóa đơn', icon: receiptOutline, route: '/orders', show: hasPermission(permissions, '/api/v1/orders', 'GET') },
    { label: 'Khác', icon: reorderThreeOutline, route: '/more', show: true },
  ].filter(item => item.show);

  return (
    <IonPage className="home-page">
      <IonHeader className="ion-no-border home-header">
        <IonToolbar>
          <div className="header-container">
            <div className="header-left">
              <div className="logo-icon-box">
                <IonIcon icon={storefront} />
              </div>
              <span className="brand-name">{storeName || 'ShopLite'}</span>
              {memberRole ? <span className="sub-label">{memberRole}</span> : null}
            </div>
            <div className="header-right">
              <IonIcon icon={callOutline} className="header-action-icon" />
              <button className="notification-button" type="button" onClick={toggleNotifications} aria-label="Notifications">
                <IonIcon icon={notificationsOutline} className="header-action-icon" />
                {unreadCount > 0 ? <span className="notification-badge">{unreadCount}</span> : null}
              </button>
              <IonIcon icon={mailOutline} className="header-action-icon" />
            </div>
          </div>
        </IonToolbar>
      </IonHeader>

      <IonContent className="home-content">
        <div className="home-container">
          <div className="revenue-main-card">
            <div className="revenue-info">
              <p className="label1">Doanh Thu Hôm Nay</p>
              <h2 className="amount">{fmt(todayRevenue)}d</h2>
              <div className="trend-badge">
                <IonIcon icon={cashOutline} />
                <span>Cap nhat moi nhat</span>
              </div>
            </div>
            <IonIcon icon={cashOutline} className="bg-icon" />
          </div>

          <div className="stats-row">
            <div className="stats-card">
              <p className="label1">Đơn hàng hôm nay</p>
              <h3 className="value">{todayOrdersCount}</h3>
              <p className="sub-label">Hoàn thành</p>
            </div>
            <div className="stats-card">
              <p className="label1">Lợi nhuận hôm nay</p>
              <h3 className="value">{fmt(todayProfit)}d</h3>
              <p className="sub-label warning">Tỷ suất ~30%</p>
            </div>
          </div>

          <div className="section-container">
            <h4 className="section-title">Phím tắt nhanh</h4>
            <div className="shortcut-grid">
              {shortcuts.length === 0 ? (
                <div className="shortcut-empty">Không có chức năng khả dụng</div>
              ) : shortcuts.map(item => (
                <div className="shortcut-item" key={item.route} onClick={() => history.push(item.route)}>
                  <div className={`icon-box ${item.color}`}><IonIcon icon={item.icon} /></div>
                  <span>{item.label}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="section-container">
            <div className="section-header">
              <h4 className="section-title">Đơn hàng mới</h4>
              <span className="view-all" onClick={() => history.push('/orders')}>Xem tất cả</span>
            </div>

            {recentOrders.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '16px', color: '#6b7280', fontSize: '14px' }}>
                Chưa có đơn hàng nào
              </div>
            ) : recentOrders.map((o, idx) => {
              const timeStr = o.createdAt ? new Date(o.createdAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'}) : '';
              const isCompleted = o.status === 'COMPLETED';
              const statusColor = isCompleted ? 'blue' : o.status === 'PENDING_PAYMENT' ? 'orange' : 'red';
              const statusText = isCompleted ? 'Hoan thanh' : o.status === 'PENDING_PAYMENT' ? 'Cho thanh toan' : 'Nhap';
              const itemsCount = o.items?.reduce((sum, i) => sum + (i.quantity ?? 1), 0) || 0;

              return (
                <div className="order-card" key={o.id || idx}>
                  <div className={`order-user-icon ${statusColor}`}><IonIcon icon={personCircleOutline} /></div>
                  <div className="order-info">
                    <div className="order-row">
                      <span className="name">{o.customerName || 'Khach le'}</span>
                      <span className="amount">{fmt(o.totalAmount)}d</span>
                    </div>
                    <div className="order-row">
                      <span className="detail">{timeStr} - {itemsCount} san pham</span>
                      <span className={`status-badge ${statusColor}`}>{statusText}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="bottom-spacer"></div>
        </div>
      </IonContent>

      <div className="custom-tab-bar">
        {tabs.map(item => (
          <div
            key={item.route}
            className={`tab-item ${item.active ? 'active' : ''}`}
            role="button"
            tabIndex={0}
            onClick={() => !item.active && history.push(item.route)}
          >
            <IonIcon icon={item.icon} />
            <span>{item.label}</span>
          </div>
        ))}
      </div>
      {notificationOpen ? (
        <div className="notification-overlay" onClick={() => setNotificationOpen(false)}>
          <div className="notification-panel" onClick={event => event.stopPropagation()}>
            <div className="notification-panel-head">
              <div className="notification-panel-title">Thong bao</div>
              <button type="button" className="notification-close" onClick={() => setNotificationOpen(false)}>x</button>
            </div>
            {notifications.length === 0 ? (
              <div className="notification-empty">Chua co thong bao</div>
            ) : notifications.map(item => {
              const invitation = item.invitation;
              const canAct = item.type === 'STORE_INVITATION'
                && !item.actionTaken
                && invitation?.status === 'PENDING';
              return (
                <div className="notification-item" key={item.id}>
                  <div className="notification-title">{item.title}</div>
                  <div className="notification-message">{item.message}</div>
                  {invitation ? (
                    <div className="notification-meta">
                      {invitation.storeName} - {invitation.roleName}
                    </div>
                  ) : null}
                  {canAct ? (
                    <div className="notification-actions">
                      <button type="button" className="notification-accept" onClick={() => handleAcceptInvitation(item)}>
                        Accept
                      </button>
                      <button type="button" className="notification-decline" onClick={() => handleDeclineInvitation(item)}>
                        Decline
                      </button>
                    </div>
                  ) : item.actionTaken ? (
                    <div className="notification-done">Da xu ly</div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      ) : null}
      <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
    </IonPage>
  );
};

export default Home;
