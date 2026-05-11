import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonToast,
  IonToolbar,
  IonModal
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
  notifications as notificationsIcon,
  mailOutline,
  chevronBackOutline,
  storefront
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
  import { useIonViewWillEnter } from '@ionic/react';
import { authApis, endpoints } from '../utils/Apis';
import { getCurrentMe, ME_SESSION_UPDATED_EVENT, readStoredCurrentStore } from '../utils/meSession';
import { notificationService } from '../services/notification.service';
import { storeInvitationService } from '../services/storeInvitation.service';
import { hasPermission } from '../utils/permissions';
import { getMenuIcon, getMenusByType, getMenuTitle, hasMenuPayload } from '../utils/menuAccess';
import type { Menu, Notification, Order, Permission } from '../api/types';
import './Home.css';

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const shortcutColors: Record<string, string> = {
  SHORTCUT_SALES: 'blue',
  SHORTCUT_PRODUCTS: 'indigo',
  SHORTCUT_EMPLOYEES: 'orange',
  SHORTCUT_IMPORT_ORDERS: 'red',
};

const getInitialStore = () => readStoredCurrentStore();
const nowMs = () => Math.round(performance.now());

const Home: React.FC = () => {
  const history = useHistory();
  const pageStartRef = useState(() => nowMs())[0];
  const initialStore = getInitialStore();

  const [todayRevenue, setTodayRevenue] = useState(0);
  const [todayOrdersCount, setTodayOrdersCount] = useState(0);
  const [todayProfit, setTodayProfit] = useState(0);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [storeName, setStoreName] = useState(initialStore?.name ?? '');
  const [memberRole, setMemberRole] = useState(initialStore?.memberRole ?? '');
  const [permissions, setPermissions] = useState<Permission[]>(() => (initialStore?.permissions || []) as Permission[]);
  const [menus, setMenus] = useState<Menu[]>(() => (initialStore?.menus || []) as Menu[]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const unreadCount = notifications.filter(n => !n.read).length;
  const lastLoadedRef = useRef<number>(0);
  const DATA_STALE_MS = 60_000; // re-fetch after 60 s

  const logPerf = (message: string) => {
    console.info(`[Home perf] +${nowMs() - pageStartRef}ms ${message}`);
  };

  useEffect(() => {
    logPerf('mounted');
    const hydrateFromStoredStore = () => {
      const store = readStoredCurrentStore();
      if (!store) return;
      setStoreName(store.name || 'Chua co cua hang');
      setMemberRole(store.memberRole || '');
      setPermissions((store.permissions || []) as Permission[]);
      setMenus((store.menus || []) as Menu[]);
      logPerf(`hydrated from cache: menus=${store.menus?.length ?? 0}, permissions=${store.permissions?.length ?? 0}`);
    };

    globalThis.addEventListener(ME_SESSION_UPDATED_EVENT, hydrateFromStoredStore);
    return () => {
      globalThis.removeEventListener(ME_SESSION_UPDATED_EVENT, hydrateFromStoredStore);
    };
  }, []);

  const refreshNotifications = async () => {
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

    const items = await refreshNotifications();
    const unread = items.filter(n => !n.read);
    if (unread.length > 0) {
      void Promise.all(unread.map(n => notificationService.markRead(n.id)))
        .then(refreshNotifications)
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
      setMenus(result.currentStore?.menus || []);
      setToast('Đã tham gia cửa hàng');
      await refreshNotifications();
      history.replace('/home');
    } catch (err: any) {
      setToast(err.message || 'Không thể chấp nhận lời mời');
    }
  };

  const handleDeclineInvitation = async (notification: Notification) => {
    if (!notification.referenceId) return;
    try {
      await storeInvitationService.decline(notification.referenceId);
      setToast('Đã từ chối lời mời');
      await refreshNotifications();
    } catch (err: any) {
      setToast(err.message || 'Không thể từ chối lời mời');
    }
  };

  const loadData = async () => {
    try {
      const dashboardStart = nowMs();
      const res = await authApis().get<any>(endpoints['dashboard-today']);
      const data = res.data?.data ?? res.data;

      if (data?.user) {
        setStoreName(data.currentStore?.name || 'Chưa có cửa hàng');
        setMemberRole(data.currentStore?.memberRole || '');
        setPermissions((data.currentStore?.permissions || []) as Permission[]);
        setMenus((data.currentStore?.menus || []) as Menu[]);
      }

      if (data?.todayStats) {
        setTodayRevenue(data.todayStats.revenue ?? 0);
        setTodayOrdersCount(data.todayStats.orderCount ?? 0);
        setTodayProfit(data.todayStats.profit ?? 0);
        setRecentOrders(data.todayStats.recentOrders ?? []);
      }

      if (Array.isArray(data?.notifications)) {
        setNotifications(data.notifications);
      }

      lastLoadedRef.current = nowMs();
      logPerf(`dashboard loaded in ${nowMs() - dashboardStart}ms`);
    } catch (err) {
      console.error(err);
    }
  };

  useIonViewWillEnter(() => {
    logPerf('ion view will enter');

    const now = nowMs();
    const isStale = now - lastLoadedRef.current > DATA_STALE_MS;
    if (!isStale) return;

    loadData();
  });

  const shortcuts = hasMenuPayload(menus)
    ? getMenusByType(menus, 'SHORTCUT')
        .filter(item => item.route)
        .map(item => ({
          label: getMenuTitle(item),
          icon: getMenuIcon(item.icon, cartOutline),
          color: shortcutColors[item.code] || 'blue',
          route: item.route || '/home',
        }))
    : [
        { label: 'Tạo đơn', icon: cartOutline, color: 'blue', route: '/sales', show: hasPermission(permissions, '/api/v1/orders', 'POST') },
        { label: 'Sản phẩm', icon: cubeOutline, color: 'indigo', route: '/products', show: hasPermission(permissions, '/api/v1/products', 'GET') },
        { label: 'Nhân viên', icon: peopleOutline, color: 'orange', route: '/employees', show: hasPermission(permissions, '/api/v1/employees', 'GET') },
        { label: 'Nhập hàng', icon: logInOutline, color: 'red', route: '/import-orders', show: hasPermission(permissions, '/api/v1/import-orders', 'GET') },
      ].filter(item => item.show);

  const tabs = hasMenuPayload(menus) && getMenusByType(menus, 'TAB').filter(item => item.route).length >= 4
    ? getMenusByType(menus, 'TAB')
        .filter(item => item.route)
        .map(item => ({
          label: getMenuTitle(item),
          icon: getMenuIcon(item.icon, homeOutline),
          active: item.route === '/home',
          route: item.route || '/home',
        }))
    : [
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
        <IonRefresher slot="fixed" onIonRefresh={async (e) => { await loadData(); e.detail.complete(); }}>
          <IonRefresherContent />
        </IonRefresher>
        <div className="home-container">
          <div className="revenue-main-card">
            <div className="revenue-info">
              <p className="label1">Doanh Thu Hôm Nay</p>
              <h2 className="amount">{fmt(todayRevenue)}đ</h2>
              <div className="trend-badge">
                <IonIcon icon={cashOutline} />
                <span>Cập nhật mới nhất</span>
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
              <h3 className="value">{fmt(todayProfit)}đ</h3>
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
      <IonModal isOpen={notificationOpen} onDidDismiss={() => setNotificationOpen(false)} className="notification-modal">
        <div className="noti-header">
          <button className="noti-back-btn" onClick={() => setNotificationOpen(false)}>
            <IonIcon icon={chevronBackOutline} />
          </button>
          <span className="noti-header-title">Thông báo</span>
        </div>
        <IonContent style={{ '--background': '#f4f6f9' }}>
          <div className="noti-list">
            {notifications.length === 0 ? (
              <div className="notification-empty">Chưa có thông báo</div>
            ) : notifications.map(item => {
              const invitation = item.invitation;
              const canAct = item.type === 'STORE_INVITATION'
                && !item.actionTaken
                && invitation?.status === 'PENDING';
              
              const dateObj = new Date(item.createdAt);
              const timeStr = dateObj.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
              const isToday = dateObj.toDateString() === new Date().toDateString();
              const dateDisplay = `${timeStr} ${isToday ? 'Hôm nay' : dateObj.toLocaleDateString('vi-VN')}`;

              return (
                <div className="noti-item" key={item.id}>
                  <div className="noti-icon-wrap">
                    <div className="noti-icon-bg">
                      <IonIcon icon={notificationsIcon} className="noti-icon" />
                    </div>
                  </div>
                  <div className="noti-content-wrap">
                    <div className="noti-time">{dateDisplay}</div>
                    <div className="noti-card">
                      <div className="noti-card-title">{item.title}</div>
                      <div className="noti-card-message">{item.message}</div>
                      {canAct ? (
                        <div className="noti-actions">
                          <button type="button" className="noti-btn-decline" onClick={() => handleDeclineInvitation(item)}>
                            Từ chối
                          </button>
                          <button type="button" className="noti-btn-accept" onClick={() => handleAcceptInvitation(item)}>
                            Xác nhận
                          </button>
                        </div>
                      ) : item.actionTaken ? (
                        <div className="noti-done">Đã xử lý</div>
                      ) : null}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </IonContent>
      </IonModal>
      <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
    </IonPage>
  );
};

export default Home;
