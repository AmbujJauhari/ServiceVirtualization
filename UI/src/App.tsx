import React from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import RestDashboard from './pages/rest/RestDashboard';
import SoapDashboard from './pages/soap/SoapDashboard';
import TibcoDashboard from './pages/tibco/TibcoDashboard';
import StubForm from './pages/rest/stubs/StubForm';
import SoapStubForm from './pages/soap/stubs/SoapStubForm';
import TibcoStubForm from './pages/tibco/stubs/TibcoStubForm';
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
    
    if (path === '/soap') {
      return {
        title: 'SOAP API Management',
        subtitle: 'Configure and manage SOAP web service virtual endpoints'
      };
    }
    
    if (path === '/tibco') {
      return {
        title: 'TIBCO EMS Management',
        subtitle: 'Configure and manage TIBCO EMS messaging services'
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
    
    if (path === '/soap/stubs/new') {
      return {
        title: 'Create New SOAP Stub',
        subtitle: 'Define a new SOAP web service stub'
      };
    }
    
    if (path.match(/\/soap\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit SOAP Stub',
        subtitle: 'Modify an existing SOAP web service stub'
      };
    }
    
    if (path === '/tibco/stubs/create') {
      return {
        title: 'Create TIBCO Message Stub',
        subtitle: 'Define a new TIBCO message stub for virtual messaging'
      };
    }
    
    if (path.match(/\/tibco\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit TIBCO Message Stub',
        subtitle: 'Modify an existing TIBCO message stub'
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
        <Route path="/soap" element={<SoapDashboard />} />
        <Route path="/tibco" element={<TibcoDashboard />} />
        <Route path="/tibco/stubs/create" element={<TibcoStubForm />} />
        <Route path="/tibco/stubs/:id/edit" element={<TibcoStubForm isEdit={true} />} />
        <Route path="/rest/stubs/new" element={<StubForm />} />
        <Route path="/rest/stubs/:id/edit" element={<StubForm isEdit={true} />} />
        <Route path="/soap/stubs/new" element={<SoapStubForm />} />
        <Route path="/soap/stubs/:id/edit" element={<SoapStubForm isEdit={true} />} />
      </Routes>
    </MainLayout>
  );
};

export default App; 