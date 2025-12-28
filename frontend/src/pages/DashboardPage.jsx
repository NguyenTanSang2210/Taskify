import React, { useState, useEffect } from 'react';
import { dashboardApi } from '../api/dashboardApi';
import InlineNotice from '../components/InlineNotice';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function DashboardPage() {
    const { user } = useAuth();
    const roleName = typeof user?.role === 'object' && user?.role ? user.role.name : user?.role;

    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!user) return;
        const fetchStats = async () => {
            try {
                let data;
                if (roleName === 'ADMIN') {
                    data = await dashboardApi.getAdminStats();
                } else if (roleName === 'LECTURER') {
                    data = await dashboardApi.getLecturerStats();
                }
                setStats(data);
            } catch {
                setError('Không thể tải dữ liệu thống kê');
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, [user, roleName]);

    if (!user) return <div className="container py-4">Vui lòng đăng nhập</div>;

    if (loading) return <div className="container py-4">Đang tải thống kê...</div>;

    return (
        <div className="container py-4">
            <h3 className="mb-4">Tổng quan</h3>
            {error && <InlineNotice type="danger" message={error} onClose={() => setError(null)} />}
            
            <div className="row g-4">
                {roleName === 'ADMIN' && stats && (
                    <>
                        <div className="col-md-3">
                            <div className="card text-white bg-primary h-100">
                                <div className="card-body">
                                    <h5 className="card-title">Tổng số người dùng</h5>
                                    <p className="card-text display-4">{stats.totalUsers}</p>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-3">
                            <div className="card text-white bg-success h-100">
                                <div className="card-body">
                                    <h5 className="card-title">Tổng số đề tài</h5>
                                    <p className="card-text display-4">{stats.totalTopics}</p>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-3">
                            <div className="card text-white bg-info h-100">
                                <div className="card-body">
                                    <h5 className="card-title">Đề tài đang mở</h5>
                                    <p className="card-text display-4">{stats.openTopics}</p>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-3">
                            <div className="card text-white bg-warning h-100">
                                <div className="card-body">
                                    <h5 className="card-title">Tổng sinh viên</h5>
                                    <p className="card-text display-4">{stats.totalStudents}</p>
                                </div>
                            </div>
                        </div>
                        
                        <div className="col-12 mt-4">
                            <div className="card">
                                <div className="card-header fw-bold">Quản trị nhanh</div>
                                <div className="card-body">
                                    <Link to="/admin" className="btn btn-outline-primary me-2">Quản lý người dùng</Link>
                                </div>
                            </div>
                        </div>
                    </>
                )}

                {roleName === 'LECTURER' && stats && (
                    <>
                        <div className="col-md-4">
                            <div className="card text-white bg-primary h-100">
                                <div className="card-body">
                                    <h5 className="card-title">Đề tài của tôi</h5>
                                    <p className="card-text display-4">{stats.myTopics}</p>
                                </div>
                            </div>
                        </div>
                        <div className="col-12 mt-4">
                            <div className="card">
                                <div className="card-header fw-bold">Lối tắt giảng viên</div>
                                <div className="card-body">
                                    <Link to="/lecturer-topics" className="btn btn-outline-primary me-2">Quản lý đề tài</Link>
                                    <Link to="/lecturer-progress" className="btn btn-outline-success me-2">Nhận xét tiến độ</Link>
                                </div>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
