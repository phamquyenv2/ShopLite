import React, { useEffect, useRef, useState } from 'react';
import { IonIcon, IonSpinner, IonToast } from '@ionic/react';
import {
    addOutline,
    chevronBackOutline,
    searchOutline,
} from 'ionicons/icons';
import type { Supplier } from '../api/types';
import { supplierService } from '../services/supplier.service';
import './CategoryPickerModal.css'; // reuse same styles

interface SupplierPickerModalProps {
    isOpen: boolean;
    selected: number | '';
    onClose: () => void;
    onSelect: (supplier: Supplier) => void;
}

type SubView = 'list' | 'add-new';

const SupplierPickerModal: React.FC<SupplierPickerModalProps> = ({
    isOpen,
    selected,
    onClose,
    onSelect,
}) => {
    const [view, setView] = useState<SubView>('list');
    const [suppliers, setSuppliers] = useState<Supplier[]>([]);
    const [loadingList, setLoadingList] = useState(false);
    const [keyword, setKeyword] = useState('');

    // Add-new form
    const [newName, setNewName] = useState('');
    const [newPhone, setNewPhone] = useState('');
    const [newAddress, setNewAddress] = useState('');
    const [newEmail, setNewEmail] = useState('');

    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const searchRef = useRef<HTMLInputElement>(null);

    const reload = async () => {
        setLoadingList(true);
        try {
            const raw = await supplierService.getAll();
            if (Array.isArray(raw)) setSuppliers(raw);
        } finally {
            setLoadingList(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            setView('list');
            setKeyword('');
            setNewName('');
            setNewPhone('');
            setNewAddress('');
            setNewEmail('');
            void reload();
        }
    }, [isOpen]);

    const filtered = suppliers.filter((s) => {
        const query = keyword.toLowerCase();
        return (
            s.name.toLowerCase().includes(query) ||
            (s.phone && s.phone.includes(query)) ||
            (s.id.toString().includes(query))
        );
    });

    const handleAddNew = async () => {
        if (!newName.trim()) {
            setToast('Vui lòng nhập tên nhà cung cấp');
            return;
        }
        setSaving(true);
        try {
            const savedSupplier = await supplierService.create({
                name: newName.trim(),
                phone: newPhone.trim() || undefined,
                address: newAddress.trim() || undefined,
                email: newEmail.trim() || undefined,
            });

            setToast('Thêm nhà cung cấp thành công');
            setNewName('');
            setNewPhone('');
            setNewAddress('');
            setNewEmail('');
            await reload();
            if (savedSupplier && savedSupplier.id) {
                onSelect(savedSupplier);
            } else {
                setView('list');
            }
        } catch (error: any) {
            setToast(error?.message || 'Thêm nhà cung cấp thất bại');
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="cpm-overlay">
            <div className={`cpm-sheet ${isOpen ? 'cpm-sheet--in' : ''}`}>
                {/* ───── LIST VIEW ───── */}
                {view === 'list' && (
                    <>
                        <div className="cpm-header">
                            <button className="cpm-back-btn" onClick={onClose}>
                                <IonIcon icon={chevronBackOutline} />
                            </button>
                            <span className="cpm-title">Nhà cung cấp</span>
                            <button
                                className="cpm-add-btn"
                                onClick={() => { 
                                    setNewName(''); 
                                    setNewPhone(''); 
                                    setNewAddress(''); 
                                    setNewEmail(''); 
                                    setView('add-new'); 
                                }}
                            >
                                <IonIcon icon={addOutline} />
                            </button>
                        </div>

                        <div className="cpm-search-bar">
                            <IonIcon icon={searchOutline} className="cpm-search-icon" />
                            <input
                                ref={searchRef}
                                className="cpm-search-input"
                                placeholder="Tên, mã, điện thoại NCC"
                                value={keyword}
                                onChange={(e) => setKeyword(e.target.value)}
                            />
                        </div>

                        <div className="cpm-list-wrap">
                            {loadingList ? (
                                <div className="cpm-center">
                                    <IonSpinner name="crescent" color="primary" />
                                </div>
                            ) : filtered.length === 0 ? (
                                <div className="cpm-empty">Không tìm thấy nhà cung cấp</div>
                            ) : (
                                <div className="cpm-card">
                                    {filtered.map((supplier, idx) => (
                                        <button
                                            key={supplier.id}
                                            className={`cpm-item ${selected === supplier.id ? 'cpm-item--active' : ''} ${idx < filtered.length - 1 ? 'cpm-item--divider' : ''}`}
                                            onClick={() => onSelect(supplier)}
                                        >
                                            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                                                <span>{supplier.name}</span>
                                                {supplier.phone && (
                                                    <span style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>
                                                        {supplier.phone}
                                                    </span>
                                                )}
                                            </div>
                                            {selected === supplier.id && (
                                                <span className="cpm-item-check">✓</span>
                                            )}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                )}

                {/* ───── ADD NEW VIEW ───── */}
                {view === 'add-new' && (
                    <>
                        <div className="cpm-header">
                            <button className="cpm-back-btn" onClick={() => setView('list')}>
                                <IonIcon icon={chevronBackOutline} />
                            </button>
                            <span className="cpm-title">Thêm nhà cung cấp</span>
                            <button
                                className="cpm-save-text-btn"
                                onClick={handleAddNew}
                                disabled={saving}
                            >
                                {saving ? <IonSpinner name="dots" /> : 'Lưu'}
                            </button>
                        </div>

                        <div className="cpm-form-card">
                            <div className="cpm-form-field">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Tên nhà cung cấp *"
                                    value={newName}
                                    onChange={(e) => setNewName(e.target.value)}
                                    autoFocus
                                />
                            </div>
                            <div className="cpm-form-field">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Số điện thoại"
                                    type="tel"
                                    value={newPhone}
                                    onChange={(e) => setNewPhone(e.target.value)}
                                />
                            </div>
                            <div className="cpm-form-field">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Địa chỉ"
                                    value={newAddress}
                                    onChange={(e) => setNewAddress(e.target.value)}
                                />
                            </div>
                            <div className="cpm-form-field cpm-form-field--last">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Email"
                                    type="email"
                                    value={newEmail}
                                    onChange={(e) => setNewEmail(e.target.value)}
                                />
                            </div>
                        </div>
                    </>
                )}
            </div>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </div>
    );
};

export default SupplierPickerModal;
