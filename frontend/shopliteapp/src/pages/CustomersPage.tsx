import React, { useMemo, useState } from 'react';
import {
    IonPage,
    IonHeader,
    IonToolbar,
    IonContent,
    IonIcon,
    IonButtons,
    IonButton,
    IonSpinner,
    IonToast,
    IonFab,
    IonFabButton,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    chevronBackOutline,
    searchOutline,
    addOutline,
    chevronDownOutline,
    checkmarkOutline,
    closeOutline,
    personOutline,
} from 'ionicons/icons';
import { customerService } from '../services/customer.service';
import type { Customer } from '../api/types';
import './CustomersPage.css';

const fmt = (n: number) => n.toLocaleString('vi-VN');

type PeriodFilter = 'all' | 'today' | 'yesterday' | '7days' | '30days';

const PERIOD_LABELS: { value: PeriodFilter; label: string }[] = [
    { value: 'all', label: 'Toàn thời gian' },
    { value: 'today', label: 'Hôm nay' },
    { value: 'yesterday', label: 'Hôm qua' },
    { value: '7days', label: '7 ngày qua' },
    { value: '30days', label: '30 ngày qua' },
];

const CustomersPage: React.FC = () => {
    const ionRouter = useIonRouter();

    const [customers, setCustomers] = useState<Customer[]>([]);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [showSearch, setShowSearch] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    const [period, setPeriod] = useState<PeriodFilter>('all');
    const [showPeriod, setShowPeriod] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const list = await customerService.getCustomers();
            setCustomers(list);
        } catch (err: any) {
            setToast(err.message || 'Không thể tải danh sách khách hàng');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { void loadData(); });

    const filteredCustomers = useMemo(() => {
        if (!searchQuery.trim()) return customers;
        const q = searchQuery.toLowerCase().trim();
        return customers.filter(
            c => c.name.toLowerCase().includes(q) || (c.phone ?? '').includes(q)
        );
    }, [customers, searchQuery]);

    const totalAmount = useMemo(
        () => filteredCustomers.reduce((s, c) => s + (c.points ?? 0), 0),
        [filteredCustomers]
    );

    const activePeriodLabel =
        PERIOD_LABELS.find(p => p.value === period)?.label ?? 'Toàn thời gian';

    const grouped = useMemo(() => {
        const today = new Date();
        const label = `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}/${today.getFullYear()}`;
        return new Map([[label, filteredCustomers]]);
    }, [filteredCustomers]);

    return (
        <IonPage className="cust-page">
            <IonHeader className="cust-header ion-no-border">
                <IonToolbar className="cust-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="cust-title">Khách hàng</div>
                    <IonButtons slot="end">
                        <IonButton color="dark" onClick={() => setShowSearch(v => !v)}>
                            <IonIcon icon={searchOutline} style={{ fontSize: '22px' }} />
                        </IonButton>
                        <IonButton color="dark">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M7 16V4M7 4L3 8M7 4L11 8M17 8V20M17 20L21 16M17 20L13 16" />
                            </svg>
                        </IonButton>
                    </IonButtons>
                </IonToolbar>

                {showSearch && (
                    <div className="cust-search-bar">
                        <IonIcon icon={searchOutline} />
                        <input
                            autoFocus
                            type="text"
                            placeholder="Tìm theo tên hoặc số điện thoại..."
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                        {searchQuery && (
                            <button className="cust-search-clear" onClick={() => setSearchQuery('')}>
                                <IonIcon icon={closeOutline} />
                            </button>
                        )}
                    </div>
                )}

                <div className="cust-filter-bar">
                    <button className="cust-filter-icon-btn">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="4" y1="6" x2="20" y2="6" />
                            <line x1="8" y1="12" x2="16" y2="12" />
                            <line x1="10" y1="18" x2="14" y2="18" />
                        </svg>
                    </button>
                    <button className="cust-period-btn" onClick={() => setShowPeriod(v => !v)}>
                        {activePeriodLabel}
                        <IonIcon icon={chevronDownOutline} />
                    </button>
                </div>

                {showPeriod && (
                    <div className="cust-period-dropdown">
                        {PERIOD_LABELS.map(opt => (
                            <div
                                key={opt.value}
                                className={`cust-period-item${period === opt.value ? ' active' : ''}`}
                                onClick={() => { setPeriod(opt.value); setShowPeriod(false); }}
                            >
                                {opt.label}
                                {period === opt.value && (
                                    <IonIcon icon={checkmarkOutline} style={{ color: '#0066FF' }} />
                                )}
                            </div>
                        ))}
                    </div>
                )}

                <div className="cust-summary">
                    <div className="cust-summary-left">
                        <div className="cust-summary-title">
                            Tổng bán
                            <IonIcon icon={chevronDownOutline} style={{ fontSize: '14px', marginLeft: '4px' }} />
                        </div>
                        <div className="cust-summary-sub">{filteredCustomers.length} khách hàng</div>
                    </div>
                    <div className="cust-summary-amount">{fmt(totalAmount)}</div>
                </div>
            </IonHeader>

            <IonContent className="cust-content">
                {showPeriod && (
                    <div className="cust-overlay" onClick={() => setShowPeriod(false)} />
                )}

                {loading ? (
                    <div className="cust-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : filteredCustomers.length === 0 ? (
                    <div className="cust-empty">
                        <IonIcon icon={personOutline} className="cust-empty-icon" />
                        <p>Chưa có khách hàng nào</p>
                    </div>
                ) : (
                    <div className="cust-list-card">
                        {Array.from(grouped.entries()).map(([label, items]) => (
                            <div key={label} className="cust-list-group">
                                <div className="cust-date-label">{label}</div>
                                {items.map((customer, idx) => (
                                    <div
                                        key={customer.id}
                                        className={`cust-list-item${idx === items.length - 1 ? ' last-item' : ''}`}
                                        onClick={() => ionRouter.push(`/customers/${customer.id}`)}
                                    >
                                        <div className="cust-avatar">
                                            <IonIcon icon={personOutline} />
                                        </div>
                                        <div className="cust-item-info">
                                            <div className="cust-item-name">{customer.name}</div>
                                            {customer.phone && (
                                                <div className="cust-item-phone">{customer.phone}</div>
                                            )}
                                        </div>
                                        <div className="cust-item-amount">
                                            {fmt(customer.points ?? 0)}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            {/* FAB → /customers/new */}
            <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '20px', marginRight: '8px' }}>
                <IonFabButton
                    className="cust-fab"
                    onClick={() => ionRouter.push('/customers/new')}
                >
                    <IonIcon icon={addOutline} style={{ fontSize: '28px' }} />
                </IonFabButton>
            </IonFab>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

export default CustomersPage;
