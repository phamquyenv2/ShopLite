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
    businessOutline,
} from 'ionicons/icons';
import { supplierService } from '../services/supplier.service';
import type { Supplier } from '../api/types';
import './SuppliersPage.css';

const fmt = (n: number) => n.toLocaleString('vi-VN');

type PeriodFilter = 'all' | 'today' | 'yesterday' | '7days' | '30days';

const PERIOD_LABELS: { value: PeriodFilter; label: string }[] = [
    { value: 'all', label: 'Toàn thời gian' },
    { value: 'today', label: 'Hôm nay' },
    { value: 'yesterday', label: 'Hôm qua' },
    { value: '7days', label: '7 ngày qua' },
    { value: '30days', label: '30 ngày qua' },
];

const SuppliersPage: React.FC = () => {
    const ionRouter = useIonRouter();

    const [suppliers, setSuppliers] = useState<Supplier[]>([]);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [showSearch, setShowSearch] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    const [period, setPeriod] = useState<PeriodFilter>('all');
    const [showPeriod, setShowPeriod] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const list = await supplierService.getAll();
            setSuppliers(list);
        } catch (err: any) {
            setToast(err.message || 'Không thể tải danh sách nhà cung cấp');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { void loadData(); });

    const filtered = useMemo(() => {
        if (!searchQuery.trim()) return suppliers;
        const q = searchQuery.toLowerCase().trim();
        return suppliers.filter(
            s => s.name.toLowerCase().includes(q)
                || (s.phone ?? '').includes(q)
                || (s.email ?? '').toLowerCase().includes(q)
        );
    }, [suppliers, searchQuery]);

    const activePeriodLabel =
        PERIOD_LABELS.find(p => p.value === period)?.label ?? 'Toàn thời gian';

    return (
        <IonPage className="sup-page">
            <IonHeader className="sup-header ion-no-border">
                <IonToolbar className="sup-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="sup-title">Nhà cung cấp</div>
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
                    <div className="sup-search-bar">
                        <IonIcon icon={searchOutline} />
                        <input
                            autoFocus
                            type="text"
                            placeholder="Tên, điện thoại nhà cung cấp..."
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                        {searchQuery && (
                            <button className="sup-search-clear" onClick={() => setSearchQuery('')}>
                                <IonIcon icon={closeOutline} />
                            </button>
                        )}
                    </div>
                )}

                <div className="sup-filter-bar">
                    <button className="sup-filter-icon-btn">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="4" y1="6" x2="20" y2="6" />
                            <line x1="8" y1="12" x2="16" y2="12" />
                            <line x1="10" y1="18" x2="14" y2="18" />
                        </svg>
                    </button>
                    <button className="sup-period-btn" onClick={() => setShowPeriod(v => !v)}>
                        {activePeriodLabel}
                        <IonIcon icon={chevronDownOutline} />
                    </button>
                </div>

                {showPeriod && (
                    <div className="sup-period-dropdown">
                        {PERIOD_LABELS.map(opt => (
                            <div
                                key={opt.value}
                                className={`sup-period-item${period === opt.value ? ' active' : ''}`}
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

                <div className="sup-summary">
                    <div className="sup-summary-left">
                        <div className="sup-summary-title">
                            Tổng mua
                            <IonIcon icon={chevronDownOutline} style={{ fontSize: '14px', marginLeft: '4px' }} />
                        </div>
                        <div className="sup-summary-sub">{filtered.length} nhà cung cấp</div>
                    </div>
                    <div className="sup-summary-amount">0</div>
                </div>
            </IonHeader>

            <IonContent className="sup-content">
                {showPeriod && (
                    <div className="sup-overlay" onClick={() => setShowPeriod(false)} />
                )}

                {loading ? (
                    <div className="sup-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : filtered.length === 0 ? (
                    <div className="sup-empty">
                        <IonIcon icon={businessOutline} className="sup-empty-icon" />
                        <p>Chưa có nhà cung cấp nào</p>
                    </div>
                ) : (
                    <div className="sup-list-card">
                        {filtered.map((s, idx) => (
                            <div
                                key={s.id}
                                className={`sup-list-item${idx === filtered.length - 1 ? ' last-item' : ''}`}
                                onClick={() => ionRouter.push(`/suppliers/${s.id}`)}
                            >
                                <div className="sup-item-info">
                                    <div className="sup-item-name">{s.name}</div>
                                    {s.phone && (
                                        <div className="sup-item-phone">{s.phone}</div>
                                    )}
                                </div>
                                <div className="sup-item-amount">0</div>
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '20px', marginRight: '8px' }}>
                <IonFabButton
                    className="sup-fab"
                    onClick={() => ionRouter.push('/suppliers/new')}
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

export default SuppliersPage;
