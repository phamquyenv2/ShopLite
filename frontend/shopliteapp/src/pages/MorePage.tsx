import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonToolbar,
  useIonViewWillEnter,
} from '@ionic/react';
import {
  archiveOutline,
  arrowUndoOutline,
  bagHandleOutline,
  bagOutline,
  barChartOutline,
  businessOutline,
  calendarOutline,
  cartOutline,
  chevronForwardOutline,
  clipboardOutline,
  documentTextOutline,
  gridOutline,
  homeOutline,
  informationCircleOutline,
  logOutOutline,
  peopleOutline,
  personCircleOutline,
  personOutline,
  receiptOutline,
  reorderThreeOutline,
  statsChartOutline,
  storefrontOutline,
  timeOutline,
  todayOutline,
  walletOutline,
} from 'ionicons/icons';
import { useEffect, useState } from 'react';
import { useHistory } from 'react-router-dom';
import type { Menu, Permission } from '../api/types';
import { useAuth } from '../auth/useAuth';
import { getCurrentMe, ME_SESSION_UPDATED_EVENT, readStoredCurrentStore } from '../utils/meSession';
import { canShowMenu, getMenuIcon, getMenusByType, getMenuTitle, hasMenuPayload } from '../utils/menuAccess';
import './MorePage.css';

type StaticItem = {
  code: string;
  label: string;
  route?: string;
  icon: string;
  iconClass: string;
  apiPath: string;
  method?: string;
};

type StaticGroup = {
  title: string;
  items: StaticItem[];
};

const iconClassByCode: Record<string, string> = {
  ITEM_CUSTOMERS: 'icon-green',
  ITEM_FUND_LEDGER: 'icon-blue',
  ITEM_PRODUCTS: 'icon-blue',
};

const routeByCode: Record<string, string> = {
  ITEM_SALES: '/sales',
  ITEM_ORDERS: '/orders',
  ITEM_ORDER_CREATE: '/orders/new',
  ITEM_FUND_LEDGER: '/fund-ledger',
  ITEM_PRODUCTS: '/products',
  ITEM_INVENTORY_ADJUSTMENTS: '/inventory-adjustments',
  ITEM_IMPORT_ORDERS: '/import-orders',
  ITEM_IMPORT_RETURN_ORDERS: '/import-return-orders',
  ITEM_CUSTOMERS: '/customers',
  ITEM_SUPPLIERS: '/suppliers',
  ITEM_EMPLOYEES: '/employees',
  ITEM_ROSTER: '/roster',
  ITEM_ATTENDANCE: '/attendance',
  ITEM_PAYROLLS: '/payrolls',
  ITEM_ROLE_MANAGEMENT: '/offices',
  ITEM_REPORT_END_OF_DAY: '/reports/end-of-day',
  ITEM_REPORT_SALES: '/reports/sales',
  ITEM_REPORT_INVENTORY: '/reports/inventory',
};

const fallbackTabs = (permissions: Permission[], menus: Menu[]) => [
  { label: 'Tổng quan', icon: homeOutline, route: '/home', show: true },
  { label: 'Hàng hóa', icon: gridOutline, route: '/products', show: canShowMenu(menus, 'MENU_PRODUCTS', permissions, '/api/v1/products') },
  { label: 'Bán hàng', icon: storefrontOutline, route: '/sales', show: canShowMenu(menus, 'MENU_SALES', permissions, '/api/v1/orders', 'POST') },
  { label: 'Hoá đơn', icon: receiptOutline, route: '/orders', show: canShowMenu(menus, 'MENU_ORDERS', permissions, '/api/v1/orders') },
  { label: 'Nhiều hơn', icon: reorderThreeOutline, route: '/more', show: true },
].filter(item => item.show);

const fallbackGroups: StaticGroup[] = [
  {
    title: 'Giao dịch',
    items: [
      { code: 'ITEM_SALES', label: 'Bán hàng', route: '/sales', icon: bagHandleOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders', method: 'POST' },
      { code: 'ITEM_ORDERS', label: 'Hóa đơn', route: '/orders', icon: receiptOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders' },
      { code: 'ITEM_ORDER_CREATE', label: 'Đặt hàng', route: '/orders/new', icon: bagOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders', method: 'POST' },
      { code: 'ITEM_FUND_LEDGER', label: 'Sổ quỹ', route: '/fund-ledger', icon: walletOutline, iconClass: 'icon-blue', apiPath: '/api/v1/fund-accounts' },
    ],
  },
  {
    title: 'Hàng hoá',
    items: [
      { code: 'ITEM_PRODUCTS', label: 'Hàng hoá', route: '/products', icon: archiveOutline, iconClass: 'icon-blue', apiPath: '/api/v1/products' },
      { code: 'ITEM_INVENTORY_ADJUSTMENTS', label: 'Kiểm kho', route: '/inventory-adjustments', icon: clipboardOutline, iconClass: 'icon-blue', apiPath: '/api/v1/inventory-adjustments' },
      { code: 'ITEM_IMPORT_ORDERS', label: 'Nhập hàng', route: '/import-orders', icon: cartOutline, iconClass: 'icon-blue', apiPath: '/api/v1/import-orders' },
      { code: 'ITEM_IMPORT_RETURN_ORDERS', label: 'Trả hàng nhập', route: '/import-return-orders', icon: arrowUndoOutline, iconClass: 'icon-blue', apiPath: '/api/v1/import-return-orders' },
    ],
  },
  {
    title: 'Đối tác',
    items: [
      { code: 'ITEM_CUSTOMERS', label: 'Khách hàng', route: '/customers', icon: personOutline, iconClass: 'icon-green', apiPath: '/api/v1/customers' },
      { code: 'ITEM_SUPPLIERS', label: 'Nhà cung cấp', route: '/suppliers', icon: storefrontOutline, iconClass: 'icon-blue', apiPath: '/api/v1/suppliers' },
    ],
  },
  {
    title: 'Nhân viên',
    items: [
      { code: 'ITEM_EMPLOYEES', label: 'Nhân viên', route: '/employees', icon: peopleOutline, iconClass: 'icon-blue', apiPath: '/api/v1/employees' },
      { code: 'ITEM_ROSTER', label: 'Lịch làm việc', route: '/roster', icon: timeOutline, iconClass: 'icon-blue', apiPath: '/api/v1/roster/day' },
      { code: 'ITEM_ATTENDANCE', label: 'Chấm công', route: '/attendance', icon: calendarOutline, iconClass: 'icon-blue', apiPath: '/api/v1/attendance' },
      { code: 'ITEM_PAYROLLS', label: 'Bảng lương', route: '/payrolls', icon: documentTextOutline, iconClass: 'icon-blue', apiPath: '/api/v1/payrolls' },
    ],
  },
  {
    title: 'Báo cáo',
    items: [
      { code: 'ITEM_REPORT_END_OF_DAY', label: 'Cuối ngày', route: '/reports/end-of-day', icon: todayOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders' },
      { code: 'ITEM_REPORT_SALES', label: 'Bán hàng', route: '/reports/sales', icon: statsChartOutline, iconClass: 'icon-green', apiPath: '/api/v1/orders' },
      { code: 'ITEM_REPORT_INVENTORY', label: 'Hàng hóa', route: '/reports/inventory', icon: barChartOutline, iconClass: 'icon-blue', apiPath: '/api/v1/products' },
    ],
  },
];

const getInitialStore = () => readStoredCurrentStore();
const nowMs = () => Math.round(performance.now());

const MorePage: React.FC = () => {
  const history = useHistory();
  const { logout } = useAuth();
  const pageStartRef = useState(() => nowMs())[0];
  const initialStore = getInitialStore();
  const [storeName, setStoreName] = useState(initialStore?.name ?? 'ShopLite');
  const [memberRole, setMemberRole] = useState(initialStore?.memberRole ?? '');
  const [permissions, setPermissions] = useState<Permission[]>(() => (initialStore?.permissions || []) as Permission[]);
  const [menus, setMenus] = useState<Menu[]>(() => (initialStore?.menus || []) as Menu[]);

  const logPerf = (message: string) => {
    console.info(`[More perf] +${nowMs() - pageStartRef}ms ${message}`);
  };

  useEffect(() => {
    logPerf('mounted');
    const hydrateFromStoredStore = () => {
      const store = readStoredCurrentStore();
      if (!store) return;
      setStoreName(store.name || 'ShopLite');
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

  useIonViewWillEnter(() => {
    logPerf('ion view will enter');
    const loadMe = async () => {
      try {
        const meStart = nowMs();
        const mePayload = await getCurrentMe();
        const currentStore = mePayload?.currentStore ?? null;
        setStoreName(currentStore?.name || 'ShopLite');
        setMemberRole(currentStore?.memberRole || '');
        setPermissions((currentStore?.permissions || []) as Permission[]);
        setMenus((currentStore?.menus || []) as Menu[]);
        logPerf(`me loaded in ${nowMs() - meStart}ms: menus=${currentStore?.menus?.length ?? 0}, permissions=${currentStore?.permissions?.length ?? 0}`);
      } catch (err) {
        console.error(err);
      }
    };
    loadMe();
  });

  const handleLogout = async () => {
    await logout();
    history.replace('/login');
  };

  const menuGroups = hasMenuPayload(menus)
    ? getMenusByType(menus, 'GROUP')
        .map(group => ({
          title: getMenuTitle(group),
          items: getMenusByType(menus, 'ITEM')
            .filter(item => item.parentId === group.id)
            .map(item => ({
              code: item.code,
              label: getMenuTitle(item),
              route: routeByCode[item.code] || item.route || undefined,
              icon: item.code === 'ITEM_ROLE_MANAGEMENT' ? businessOutline : getMenuIcon(item.icon, archiveOutline),
              iconClass: iconClassByCode[item.code] || 'icon-blue',
              apiPath: '',
            })),
        }))
        .filter(group => group.items.length > 0)
    : fallbackGroups
        .map(group => ({
          ...group,
          items: group.items.filter(item => canShowMenu(menus, item.code, permissions, item.apiPath, item.method)),
        }))
        .filter(group => group.items.length > 0);

  const tabs = hasMenuPayload(menus) && getMenusByType(menus, 'TAB').filter(item => item.route).length >= 4
    ? getMenusByType(menus, 'TAB')
        .filter(item => item.route)
        .map(item => ({
          label: item.code === 'MENU_MORE' ? 'Nhiều hơn' : getMenuTitle(item),
          icon: getMenuIcon(item.icon, homeOutline),
          route: item.route || '/home',
        }))
    : fallbackTabs(permissions, menus);

  return (
    <IonPage className="more-page">
      <IonHeader className="ion-no-border">
        <IonToolbar className="more-page-toolbar" />
      </IonHeader>

      <IonContent className="more-page-content" color="light">
        <div className="more-container">
          <div className="profile-card">
            <div className="profile-header">
              <div className="profile-info-wrap">
                <div className="avatar-box">
                  <IonIcon icon={personCircleOutline} />
                </div>
                <div className="profile-details">
                  <h2>{storeName}</h2>
                  <p>{memberRole || 'Thành viên'}</p>
                </div>
              </div>
            </div>

            <div className="profile-footer">
              <span className="store-info-text">Thông tin cửa hàng</span>
              <div className="trial-badge">
                <IonIcon icon={chevronForwardOutline} />
              </div>
            </div>
          </div>

          {menuGroups.map(group => (
            <div className="menu-group" key={group.title}>
              <h3 className="group-title">{group.title}</h3>
              <div className="grid-2-col">
                {group.items.map(item => (
                  <div
                    className="menu-grid-item"
                    key={item.code}
                    onClick={() => item.route && history.push(item.route)}
                  >
                    <IonIcon icon={item.icon} className={item.iconClass} />
                    <span>{item.label}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}

          {/* ── Báo cáo – luôn hiển thị ── */}
          <div className="menu-group">
            <h3 className="group-title">Báo cáo</h3>
            <div className="grid-2-col">
              <div
                className="menu-grid-item"
                onClick={() => history.push('/reports/end-of-day')}
              >
                <IonIcon icon={todayOutline} className="icon-blue" />
                <span>Cuối ngày</span>
              </div>
              <div
                className="menu-grid-item"
                onClick={() => history.push('/reports/sales')}
              >
                <IonIcon icon={statsChartOutline} className="icon-green" />
                <span>Bán hàng</span>
              </div>
              <div
                className="menu-grid-item"
                onClick={() => history.push('/reports/inventory')}
              >
                <IonIcon icon={barChartOutline} className="icon-blue" />
                <span>Hàng hóa</span>
              </div>
            </div>
          </div>

          <div className="menu-list-group">
            {permissions.some(p => p.apiPath.startsWith('/api/v1/employee-salaries/me')) && (
              <div className="menu-list-item" onClick={() => history.push('/my-salary')}>
                <div className="item-left">
                  <IonIcon icon={walletOutline} style={{ color: '#3b82f6' }} />
                  <span>Mức lương của tôi</span>
                </div>
                <IonIcon icon={chevronForwardOutline} className="chevron" />
              </div>
            )}
            <div className="menu-list-item">
              <div className="item-left">
                <IonIcon icon={informationCircleOutline} />
                <span>Điều khoản sử dụng</span>
              </div>
              <IonIcon icon={chevronForwardOutline} className="chevron" />
            </div>
            <div className="menu-list-item text-danger" onClick={handleLogout}>
              <div className="item-left">
                <IonIcon icon={logOutOutline} />
                <span>Đăng xuất</span>
              </div>
            </div>
          </div>

          <div className="version-info">
            <p>Phiên bản 2.5.681</p>
          </div>

          <div className="bottom-spacer"></div>
        </div>
      </IonContent>

      <div className="custom-tab-bar">
        {tabs.map(item => (
          <div
            className={`tab-item ${item.route === '/more' ? 'active' : ''}`}
            key={item.route}
            role="button"
            tabIndex={0}
            onClick={() => item.route !== '/more' && history.push(item.route)}
          >
            <IonIcon icon={item.icon} />
            <span>{item.label}</span>
          </div>
        ))}
      </div>
    </IonPage>
  );
};

export default MorePage;
