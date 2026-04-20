import {
    IonContent,
    IonHeader,
    IonIcon,
    IonPage,
    IonRefresher,
    IonRefresherContent,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { arrowBackOutline, cashOutline } from 'ionicons/icons';
import { useMemo, useState } from 'react';
import { useHistory } from 'react-router-dom';
import { authApis, endpoints, ApiError } from '../utils/Apis';
import { CART_KEY } from '../constants/storage';
import './DraftOrdersPage.css';

type ResOrderItemDTO = {
    id?: number;
    productId: number;
    productName: string;
    quantity: number;
    price: number;
    totalPrice: number;
};

type ResOrderDTO = {
    id: number;
    customerId?: number;
    customerName?: string;
    totalAmount: number;
    createdAt: string;
    items: ResOrderItemDTO[];
};

type SalesDraft = {
    customerId: number | null;
    items: {
        product: {
            id: number;
            name: string;
            sellingPrice: number;
        };
        quantity: number;
    }[];
};

const formatVnd = (amount: number): string => `${new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(amount)))}`;

const DraftOrdersPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const history = useHistory();

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [orders, setOrders] = useState<ResOrderDTO[]>([]);
    const [toast, setToast] = useState<string | null>(null);

    const load = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await authApis().get(endpoints.orders + '?statuses=DRAFT');
            const payload = res.data as any;
            const list = Array.isArray(payload?.data) ? payload.data : (Array.isArray(payload) ? payload : []);
            setOrders(list);
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Không thể tải danh sách đơn tạm');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        void load();
    });

    const groupedOrders = useMemo(() => {
        const groups: { title: string; items: ResOrderDTO[] }[] = [];
        const map: Record<string, ResOrderDTO[]> = {};

        orders.forEach((o) => {
            const dateObj = new Date(o.createdAt);
            const today = new Date();
            const isToday = dateObj.toDateString() === today.toDateString();

            const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
            const dayName = isToday ? 'Hôm nay' : days[dateObj.getDay()];

            const dateStr = dateObj.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
            const groupKey = `${dayName}, ${dateStr}`;

            if (!map[groupKey]) {
                map[groupKey] = [];
            }
            map[groupKey].push(o);
        });

        for (const [title, items] of Object.entries(map)) {
            groups.push({ title, items });
        }
        return groups;
    }, [orders]);

    const onSelectDraft = (o: ResOrderDTO) => {
        const draft: SalesDraft = {
            customerId: o.customerId || null,
            items: (o.items || []).map(it => ({
                product: {
                    id: it.productId,
                    name: it.productName,
                    sellingPrice: it.price,
                },
                quantity: it.quantity || 1
            }))
        };
        sessionStorage.setItem(CART_KEY, JSON.stringify(draft));
        history.push('/orders/new', { salesDraft: draft, draftOrderId: o.id });
    };

    const formatTime = (isoStr: string) => {
        const d = new Date(isoStr);
        return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    };

    const buildItemsSummary = (items: ResOrderItemDTO[]) => {
        if (!items || items.length === 0) return 'Không có sản phẩm';
        return items.map(i => i.productName).join('&');
    };

    return (
        <IonPage>
            <IonHeader className="ion-no-border draft-page-header">
                <IonToolbar className="draft-toolbar">
                    <div className="draft-toolbar-left" slot="start">
                        <button className="draft-toolbar-icon" type="button" aria-label="Back" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} />
                        </button>
                        <div className="draft-toolbar-title">Đơn tạm</div>
                    </div>
                </IonToolbar>
            </IonHeader>

            <IonContent className="draft-content">
                <IonRefresher slot="fixed" onIonRefresh={async (e) => { await load(); e.detail.complete(); }}>
                    <IonRefresherContent />
                </IonRefresher>

                {error && <div className="draft-error">{error}</div>}

                {!loading && orders.length === 0 && !error && (
                    <div className="draft-empty">Không có đơn hàng lưu tạm nào</div>
                )}

                {groupedOrders.map(g => (
                    <div key={g.title} className="draft-group">
                        <div className="draft-group-title">{g.title}</div>
                        {g.items.map(o => (
                            <div key={o.id} className="draft-card" onClick={() => onSelectDraft(o)}>
                                <div className="draft-card-icon">
                                    <IonIcon icon={cashOutline} />
                                </div>
                                <div className="draft-card-body">
                                    <div className="draft-card-header">
                                        <div className="draft-card-amount">{formatVnd(o.totalAmount || 0)}</div>
                                        <div className="draft-card-time">{formatTime(o.createdAt)}</div>
                                    </div>
                                    <div className="draft-card-customer">
                                        {o.customerName || 'Khách lẻ'}
                                    </div>
                                    <div className="draft-card-items">
                                        {buildItemsSummary(o.items)}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ))}

                <div className="draft-bottom-spacer" />
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default DraftOrdersPage;
