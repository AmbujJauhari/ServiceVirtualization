import React from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import RestDashboard from './pages/rest/RestDashboard';
import SoapDashboard from './pages/soap/SoapDashboard';
import TibcoDashboard from './pages/tibco/TibcoDashboard';
import KafkaDashboard from './pages/kafka/KafkaDashboard';
import FileDashboard from './pages/file/FileDashboard';
import ActiveMQDashboard from './pages/activemq/ActiveMQDashboard';
import IBMMQDashboard from './pages/ibmmq/IBMMQDashboard';
import StubForm from './pages/rest/stubs/StubForm';
import SoapStubForm from './pages/soap/stubs/SoapStubForm';
import TibcoStubForm from './pages/tibco/stubs/TibcoStubForm';
import CreateKafkaStub from './pages/kafka/stubs/CreateKafkaStub';
import EditKafkaStub from './pages/kafka/stubs/EditKafkaStub';
import CreateFileStub from './pages/file/stubs/CreateFileStub';
import EditFileStub from './pages/file/stubs/EditFileStub';
import CreateActiveMQStub from './pages/activemq/stubs/CreateActiveMQStub';
import EditActiveMQStub from './pages/activemq/stubs/EditActiveMQStub';
import CreateIBMMQStub from './pages/ibmmq/stubs/CreateIBMMQStub';
import EditIBMMQStub from './pages/ibmmq/stubs/EditIBMMQStub';
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
    
    if (path === '/kafka') {
      return {
        title: 'Kafka Management',
        subtitle: 'Configure and manage Kafka messaging services'
      };
    }
    
    if (path === '/file') {
      return {
        title: 'File Service Management',
        subtitle: 'Configure and manage file-based virtual services'
      };
    }
    
    if (path === '/activemq') {
      return {
        title: 'ActiveMQ Management',
        subtitle: 'Configure and manage ActiveMQ messaging services'
      };
    }
    
    if (path === '/ibmmq') {
      return {
        title: 'IBM MQ Management',
        subtitle: 'Configure and manage IBM MQ messaging services'
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
    
    if (path === '/kafka/stubs/create') {
      return {
        title: 'Create Kafka Message Stub',
        subtitle: 'Define a new Kafka message stub for virtual messaging'
      };
    }
    
    if (path.match(/\/kafka\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit Kafka Message Stub',
        subtitle: 'Modify an existing Kafka message stub'
      };
    }
    
    if (path === '/activemq/stubs/create') {
      return {
        title: 'Create ActiveMQ Message Stub',
        subtitle: 'Define a new ActiveMQ message stub for virtual messaging'
      };
    }
    
    if (path.match(/\/activemq\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit ActiveMQ Message Stub',
        subtitle: 'Modify an existing ActiveMQ message stub'
      };
    }
    
    if (path === '/ibmmq/stubs/create') {
      return {
        title: 'Create IBM MQ Message Stub',
        subtitle: 'Define a new IBM MQ message stub for virtual messaging'
      };
    }
    
    if (path.match(/\/ibmmq\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit IBM MQ Message Stub',
        subtitle: 'Modify an existing IBM MQ message stub'
      };
    }
    
    if (path === '/rest/configs/new') {
      return {
        title: 'Create Recording Configuration',
        subtitle: 'Set up a new REST API recording configuration'
      };
    }
    
    if (path === '/file/stubs/create') {
      return {
        title: 'Create File Stub',
        subtitle: 'Define a new file stub for virtual file services'
      };
    }
    
    if (path.match(/\/file\/stubs\/[^/]+\/edit$/)) {
      return {
        title: 'Edit File Stub',
        subtitle: 'Modify an existing file stub'
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
        <Route path="/kafka" element={<KafkaDashboard />} />
        <Route path="/file" element={<FileDashboard />} />
        <Route path="/activemq" element={<ActiveMQDashboard />} />
        <Route path="/ibmmq" element={<IBMMQDashboard />} />
        <Route path="/tibco/stubs/create" element={<TibcoStubForm />} />
        <Route path="/tibco/stubs/:id/edit" element={<TibcoStubForm isEdit={true} />} />
        <Route path="/rest/stubs/new" element={<StubForm />} />
        <Route path="/rest/stubs/:id/edit" element={<StubForm isEdit={true} />} />
        <Route path="/soap/stubs/new" element={<SoapStubForm />} />
        <Route path="/soap/stubs/:id/edit" element={<SoapStubForm isEdit={true} />} />
        <Route path="/kafka/stubs/create" element={<CreateKafkaStub />} />
        <Route path="/kafka/stubs/:id/edit" element={<EditKafkaStub />} />
        <Route path="/file/stubs/create" element={<CreateFileStub />} />
        <Route path="/file/stubs/:id/edit" element={<EditFileStub />} />
        <Route path="/activemq/stubs/create" element={<CreateActiveMQStub />} />
        <Route path="/activemq/stubs/:id/edit" element={<EditActiveMQStub />} />
        <Route path="/ibmmq/stubs/create" element={<CreateIBMMQStub />} />
        <Route path="/ibmmq/stubs/:id/edit" element={<EditIBMMQStub />} />
      </Routes>
    </MainLayout>
  );
};

export default App; 