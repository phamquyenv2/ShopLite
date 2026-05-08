import React, { useState } from 'react';
import {
    IonContent,
    IonHeader,
    IonIcon,
    IonPage,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
    IonSpinner,
    IonToast,
    IonAlert
} from '@ionic/react';
import {
    arrowBackOutline,
    cameraOutline,
    informationCircleOutline,
    cubeOutline,
    barcodeOutline,
    trashOutline,
    ellipsisHorizontalOutline
} from 'ionicons/icons';
import { useParams } from 'react-router-dom';
import { productService } from '../services/product.service';
import type { Product, Category, ProductUpsert } from '../api/types';
import { authApis, endpoints } from '../utils/Apis';
import { uploadToCloudinary } from '../utils/cloudinary';
import { useStorePermissions } from '../utils/useStorePermissions';
import './ProductEditPage.css';

const ProductEditPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();
    const { can } = useStorePermissions();
    const canUpdateProduct = can('/api/v1/products/{id}', 'PUT');
    const canDeleteProduct = can('/api/v1/products/{id}', 'DELETE');

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [product, setProduct] = useState<Product | null>(null);
    const [categories, setCategories] = useState<Category[]>([]);

    // Form fields
    const [name, setName] = useState('');
    const [sku, setSku] = useState('');
    const [categoryId, setCategoryId] = useState<number | ''>('');
    const [sellingPrice, setSellingPrice] = useState<number | ''>('');
    const [costPrice, setCostPrice] = useState<number | ''>('');
    const [minStock, setMinStock] = useState<number | ''>('');
    const [maxStock, setMaxStock] = useState<number | ''>('');
    const [barcode, setBarcode] = useState('');
    const [unitName, setUnitName] = useState('');
    const [imageUrl, setImageUrl] = useState<string | null>(null);
    const [uploading, setUploading] = useState(false);
    const fileInputRef = React.useRef<HTMLInputElement>(null);

    const [toast, setToast] = useState<string | null>(null);
    const [deleteAlert, setDeleteAlert] = useState(false);

    useIonViewWillEnter(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [cats, p] = await Promise.all([
                    productService.getCategories(),
                    id ? productService.getProductDetail(id) : Promise.resolve(null)
                ]);
                setCategories(cats);
                if (p) {
                    setProduct(p);
                    setName(p.name || '');
                    setSku(p.sku || '');
                    setCategoryId(p.categoryId || '');
                    setSellingPrice(p.sellingPrice || '');
                    setCostPrice(p.costPrice || '');
                    setMinStock(p.minStock ?? '');
                    setMaxStock(p.maxStock ?? '');
                    setBarcode(p.barcode || '');
                    setUnitName(p.unitName || '');
                    setImageUrl(p.image ?? null);
                }
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        void load();
    });

    const handleSave = async () => {
        if (!product) return;
        if (!canUpdateProduct) {
            setToast('Bạn không có quyền sửa hàng hóa');
            return;
        }
        if (!name.trim()) {
            setToast('Vui lòng nhập tên hàng');
            return;
        }

        setSaving(true);
        try {
            const payload: ProductUpsert = {
                name,
                sku,
                categoryId: categoryId === '' ? product.categoryId : categoryId,
                unitId: product.unitId,
                stock: product.stock,
                sellingPrice: sellingPrice === '' ? 0 : sellingPrice,
                costPrice: costPrice === '' ? 0 : costPrice,
                minStock: minStock === '' ? null : minStock,
                maxStock: maxStock === '' ? null : maxStock,
                barcode,
                image: imageUrl,
                status: product.status,
                version: product.version ?? null,
            };

            await authApis().put<any>(endpoints['product-detail'](product.id), payload);
            setToast('Lưu thành công');
            setTimeout(() => {
                ionRouter.goBack();
            }, 1000);
        } catch (err: any) {
            setToast(err.message || 'Lưu thất bại');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!product) return;
        if (!canDeleteProduct) {
            setToast('Bạn không có quyền xóa hàng hóa');
            return;
        }
        setSaving(true);
        try {
            await authApis().delete<any>(endpoints['product-detail'](product.id));
            setToast('Đã xóa hàng hóa');
            setTimeout(() => {
                ionRouter.push('/products', 'root', 'replace');
            }, 1000);
        } catch (err: any) {
            setToast(err.message || 'Xóa thất bại');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <IonPage>
                <IonHeader className="ion-no-border pe-header">
                    <IonToolbar className="pe-toolbar">
                        <div className="pe-toolbar-left" slot="start">
                            <button className="pe-icon-btn" type="button" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={arrowBackOutline} />
                            </button>
                            <div className="pe-title">Sửa sản phẩm</div>
                        </div>
                    </IonToolbar>
                </IonHeader>
                <IonContent className="pe-content">
                    <div style={{ textAlign: 'center', marginTop: '40px' }}><IonSpinner /></div>
                </IonContent>
            </IonPage>
        );
    }

    if (!product) {
        return (
            <IonPage>
                <IonHeader className="ion-no-border pe-header">
                    <IonToolbar className="pe-toolbar">
                        <div className="pe-toolbar-left" slot="start">
                            <button className="pe-icon-btn" type="button" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={arrowBackOutline} />
                            </button>
                            <div className="pe-title">Sửa sản phẩm</div>
                        </div>
                    </IonToolbar>
                </IonHeader>
                <IonContent className="pe-content">
                    <div style={{ textAlign: 'center', marginTop: '40px', color: '#888' }}>
                        Không tìm thấy thông tin sản phẩm
                    </div>
                </IonContent>
            </IonPage>
        );
    }

    return (
        <IonPage className="product-edit-page">
            <IonHeader className="ion-no-border pe-header">
                <IonToolbar className="pe-toolbar">
                    <div className="pe-toolbar-left" slot="start">
                        <button className="pe-icon-btn" type="button" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} />
                        </button>
                        <div className="pe-title">Sửa sản phẩm</div>
                    </div>
                    {canUpdateProduct && (
                        <div slot="end">
                            <button className="pe-save-btn" onClick={handleSave} disabled={saving}>
                                {saving ? <IonSpinner name="dots" /> : 'Lưu'}
                            </button>
                        </div>
                    )}
                </IonToolbar>
            </IonHeader>

            <IonContent className="pe-content">
                <div className="pe-container">

                    {/* Image */}
                    <div className="pe-image-section">
                        <input
                            type="file"
                            accept="image/*"
                            ref={fileInputRef}
                            hidden
                            onChange={async (e) => {
                                const file = e.target.files?.[0];
                                if (!file) return;
                                setUploading(true);
                                try {
                                    const url = await uploadToCloudinary(file);
                                    if (url) {
                                        setImageUrl(url);
                                        setToast('Tải ảnh thành công');
                                    } else {
                                        setToast('Tải ảnh thất bại');
                                    }
                                } catch {
                                    setToast('Tải ảnh thất bại');
                                } finally {
                                    setUploading(false);
                                    e.target.value = '';
                                }
                            }}
                        />
                        <div className="pe-image-box" onClick={() => fileInputRef.current?.click()}>
                            {uploading ? (
                                <IonSpinner name="crescent" color="primary" />
                            ) : imageUrl ? (
                                <img src={imageUrl} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '12px' }} />
                            ) : (
                                <div className="pe-camera-badge"><IonIcon icon={cameraOutline} /></div>
                            )}
                        </div>
                        <div className="pe-image-text">Chạm để thay đổi hình ảnh</div>
                    </div>

                    {/* Card 1: THÔNG TIN CHUNG */}
                    <div className="pe-card">
                        <div className="pe-card-header">
                            <div className="pe-card-icon blue-bg"><IonIcon icon={informationCircleOutline} /></div>
                            THÔNG TIN CHUNG
                        </div>

                        <div className="pe-field">
                            <label className="pe-label">TÊN HÀNG</label>
                            <div className="pe-input-wrap">
                                <input className="pe-input" type="text" value={name} onChange={e => setName(e.target.value)} />
                            </div>
                        </div>

                        <div className="pe-field">
                            <label className="pe-label">SKU</label>
                            <div className="pe-input-wrap">
                                <input className="pe-input" type="text" value={sku} onChange={e => setSku(e.target.value)} />
                            </div>
                        </div>

                        <div className="pe-field">
                            <label className="pe-label">NHÓM HÀNG</label>
                            <select className="pe-select" value={categoryId} onChange={e => setCategoryId(e.target.value ? Number(e.target.value) : '')}>
                                <option value="">-- Chọn nhóm hàng --</option>
                                {categories.map(c => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Card 2: GIÁ & TỒN KHO */}
                    <div className="pe-card">
                        <div className="pe-card-header">
                            <div className="pe-card-icon blue-bg"><IonIcon icon={cubeOutline} /></div>
                            GIÁ & TỒN KHO
                        </div>

                        <div className="pe-row">
                            <div className="pe-field">
                                <label className="pe-label">GIÁ BÁN</label>
                                <div className="pe-input-wrap">
                                    <input className="pe-input has-suffix" type="number"
                                        value={sellingPrice} onChange={e => setSellingPrice(e.target.value ? Number(e.target.value) : '')} />
                                    <span className="pe-suffix">đ</span>
                                </div>
                            </div>
                            <div className="pe-field">
                                <label className="pe-label">GIÁ VỐN</label>
                                <div className="pe-input-wrap">
                                    <input className="pe-input has-suffix" type="number"
                                        value={costPrice} onChange={e => setCostPrice(e.target.value ? Number(e.target.value) : '')} />
                                    <span className="pe-suffix">đ</span>
                                </div>
                            </div>
                        </div>

                        <div className="pe-field">
                            <label className="pe-label" style={{ textAlign: 'center' }}>TỒN KHO HIỆN TẠI</label>
                            <div className="pe-disabled-valBox">
                                {product.stock || 0}
                            </div>
                            <span className="pe-sub-label">Không thể thay đổi trực tiếp tồn kho tại đây</span>
                        </div>

                        <div className="pe-row">
                            <div className="pe-field">
                                <label className="pe-label">TỒN TỐI THIỂU</label>
                                <div className="pe-input-wrap">
                                    <input className="pe-input" type="number"
                                        value={minStock} onChange={e => setMinStock(e.target.value ? Number(e.target.value) : '')} />
                                </div>
                            </div>
                            <div className="pe-field">
                                <label className="pe-label">TỒN TỐI ĐA</label>
                                <div className="pe-input-wrap">
                                    <input className="pe-input" type="number"
                                        value={maxStock} onChange={e => setMaxStock(e.target.value ? Number(e.target.value) : '')} />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Card 3: THÔNG TIN BỔ SUNG */}
                    <div className="pe-card">
                        <div className="pe-card-header">
                            <div className="pe-card-icon dots"><IonIcon icon={ellipsisHorizontalOutline} /></div>
                            THÔNG TIN BỔ SUNG
                        </div>

                        <div className="pe-field">
                            <label className="pe-label">MÃ VẠCH (BARCODE)</label>
                            <div className="pe-input-wrap">
                                <input className="pe-input" type="text" value={barcode} onChange={e => setBarcode(e.target.value)} />
                                <button className="pe-scan-btn"><IonIcon icon={barcodeOutline} /></button>
                            </div>
                        </div>

                        <div className="pe-field">
                            <label className="pe-label">ĐƠN VỊ TÍNH</label>
                            <div className="pe-input-wrap">
                                <input className="pe-input" type="text" value={unitName} onChange={e => setUnitName(e.target.value)} />
                            </div>
                        </div>
                    </div>

                    {canDeleteProduct && (
                        <button className="pe-delete-btn" onClick={() => setDeleteAlert(true)}>
                            <IonIcon icon={trashOutline} />
                            Xóa hàng hóa này
                        </button>
                    )}

                </div>
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />

            <IonAlert
                isOpen={deleteAlert}
                onDidDismiss={() => setDeleteAlert(false)}
                header="Xác nhận xóa"
                message="Bạn có chắc chắn muốn xóa hàng hóa này không? Hành động này không thể hoàn tác."
                buttons={[
                    { text: 'Hủy', role: 'cancel' },
                    { text: 'Xóa', role: 'destructive', handler: handleDelete }
                ]}
            />
        </IonPage>
    );
};

export default ProductEditPage;
