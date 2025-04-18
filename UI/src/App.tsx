import React from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import RestDashboard from './pages/rest/RestDashboard';
import StubForm from './pages/rest/stubs/StubForm';
import { MainLayout } from './components/layout/MainLayout';

const App: React.FC = () => {
  const location = useLocation();
  
  // Generate title and subtitle based on route
  const getPageInfo = () => {
    const path = location.pathname;
    
    if (path === '/') {
      return {
        title: 'Service Virtualization Dashboard',
        subtitle: 'Manage your virtual services across multiple protocols'
      };
    }
    
    if (path === '/rest') {
      return {
        title: 'REST API Management',
        subtitle: 'Configure and manage REST API virtual services'
      };
    }
    
    if (path.match(/\/rest\/recordings\/[^/]+$/)) {
      return {
        title: 'Recording Details',
        subtitle: 'View REST API recording details'
      };
    }
    
    if (path === '/rest/stubs/new') {
      return {
        title: 'Create New Stub',
        subtitle: 'Define a new REST API stub for virtual services'
      };
    }
    
    if (path.match(/\/rest\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit Stub',
        subtitle: 'Modify an existing REST API stub'
      };
    }
    
    if (path === '/rest/configs/new') {
      return {
        title: 'Create Recording Configuration',
        subtitle: 'Set up a new REST API recording configuration'
      };
    }
    
    return { title: '', subtitle: '' };
  };
  
  const { title, subtitle } = getPageInfo();
  
  return (
    <MainLayout title={title} subtitle={subtitle}>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/rest" element={<RestDashboard />} />
        <Route path="/rest/stubs/new" element={<StubForm />} />
        <Route path="/rest/stubs/:id/edit" element={<StubForm isEdit={true} />} />
      </Routes>
    </MainLayout>
  );
};

export default App; 