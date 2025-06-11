import React from 'react';

const About: React.FC = () => {
  const metrics = [
    { label: 'Faster Development', value: '50%', symbol: '⚡' },
    { label: 'Defect Reduction', value: '75%', symbol: '🛡️' },
    { label: 'Annual Savings', value: '$1.38M', symbol: '💰' },
    { label: 'Payback Period', value: '4.5 months', symbol: '⏱️' }
  ];

  const protocols = [
    { name: 'REST/HTTP', usage: 'Web APIs & Microservices', adoption: '95%' },
    { name: 'SOAP', usage: 'Enterprise Web Services', adoption: '60%' },
    { name: 'Apache Kafka', usage: 'Event Streaming', adoption: '80%' },
    { name: 'ActiveMQ', usage: 'Java Messaging', adoption: '40%' },
    { name: 'IBM MQ', usage: 'Enterprise Messaging', adoption: '30%' },
    { name: 'TIBCO EMS', usage: 'Legacy Integration', adoption: '20%' }
  ];

  const benefits = [
    {
      title: 'Dependency Independence',
      description: 'Test anytime, anywhere without external dependencies',
      symbol: '🖥️'
    },
    {
      title: 'Scenario Simulation',
      description: 'Test edge cases and error conditions easily',
      symbol: '💡'
    },
    {
      title: 'Cloud Native',
      description: 'Kubernetes-ready with horizontal scaling',
      symbol: '☁️'
    },
    {
      title: 'Cost Optimization',
      description: 'Reduce infrastructure and API testing costs',
      symbol: '💰'
    }
  ];

  return (
    <div className="max-w-7xl mx-auto space-y-12">
      {/* Hero Section */}
      <div className="text-center bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-gray-800 dark:to-gray-700 rounded-lg p-8">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
          Service Virtualization Platform
        </h1>
        <p className="text-xl text-gray-600 dark:text-gray-300 mb-6">
          Revolutionizing Application Testing Through Digital Service Twins
        </p>
        <div className="inline-flex items-center px-4 py-2 bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-full text-sm font-medium">
          <span className="mr-2">📊</span>
          550% ROI within 12 months
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {metrics.map((metric, index) => (
          <div key={index} className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 border border-gray-200 dark:border-gray-700">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <span className="text-3xl">{metric.symbol}</span>
              </div>
              <div className="ml-4">
                <p className="text-2xl font-bold text-gray-900 dark:text-white">{metric.value}</p>
                <p className="text-sm text-gray-600 dark:text-gray-400">{metric.label}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* What is Service Virtualization */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
          What is Service Virtualization?
        </h2>
        <p className="text-gray-600 dark:text-gray-300 text-lg leading-relaxed mb-6">
          Service Virtualization is a method to simulate the behavior of specific components in heterogeneous 
          component-based applications such as API-driven applications, cloud-based applications and 
          service-oriented architectures.
        </p>
        <div className="bg-blue-50 dark:bg-blue-900/20 border-l-4 border-blue-400 p-4 rounded">
          <p className="text-blue-800 dark:text-blue-200 font-medium">
            Our platform creates "virtual services" or "stubs" that mimic the behavior of real external 
            dependencies, allowing teams to develop and test applications independently of those dependencies.
          </p>
        </div>
      </div>

      {/* The Problem We Solve */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
          The Challenges We Solve
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center">
            <div className="bg-red-100 dark:bg-red-900/20 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">⏰</span>
            </div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">Development Delays</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              85% of development delays are caused by waiting for external dependencies
            </p>
          </div>
          <div className="text-center">
            <div className="bg-yellow-100 dark:bg-yellow-900/20 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">💸</span>
            </div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">High Costs</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              $2.3M average cost of production outages due to untested integration scenarios
            </p>
          </div>
          <div className="text-center">
            <div className="bg-orange-100 dark:bg-orange-900/20 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">📊</span>
            </div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">Limited Testing</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              60% of testing time wasted on environment setup and coordination
            </p>
          </div>
        </div>
      </div>

      {/* Multi-Protocol Support */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
          Multi-Protocol Support
        </h2>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Our platform supports 6 major communication protocols, enabling comprehensive testing across your entire technology stack.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {protocols.map((protocol, index) => (
            <div key={index} className="border border-gray-200 dark:border-gray-600 rounded-lg p-4">
              <div className="flex justify-between items-start mb-2">
                <h3 className="font-semibold text-gray-900 dark:text-white">{protocol.name}</h3>
                <span className="text-sm font-medium text-green-600 dark:text-green-400">{protocol.adoption}</span>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400">{protocol.usage}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Key Benefits */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
          Key Benefits
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {benefits.map((benefit, index) => (
            <div key={index} className="flex items-start space-x-4">
              <div className="flex-shrink-0">
                <span className="text-3xl">{benefit.symbol}</span>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 dark:text-white mb-2">{benefit.title}</h3>
                <p className="text-gray-600 dark:text-gray-300">{benefit.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ROI Breakdown */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
          Return on Investment
        </h2>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Annual Cost Savings</h3>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Developer Productivity</span>
                <span className="font-semibold text-green-600 dark:text-green-400">$480K</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Infrastructure</span>
                <span className="font-semibold text-green-600 dark:text-green-400">$400K</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Third-party API Calls</span>
                <span className="font-semibold text-green-600 dark:text-green-400">$100K</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Production Incidents</span>
                <span className="font-semibold text-green-600 dark:text-green-400">$400K</span>
              </div>
              <div className="border-t border-gray-200 dark:border-gray-600 pt-3">
                <div className="flex justify-between items-center">
                  <span className="font-semibold text-gray-900 dark:text-white">Total Annual Savings</span>
                  <span className="font-bold text-green-600 dark:text-green-400 text-lg">$1.38M</span>
                </div>
              </div>
            </div>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Investment Overview</h3>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Initial Investment</span>
                <span className="font-semibold text-gray-900 dark:text-white">$250K</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Annual Operating Cost</span>
                <span className="font-semibold text-gray-900 dark:text-white">$200K</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600 dark:text-gray-400">Payback Period</span>
                <span className="font-semibold text-blue-600 dark:text-blue-400">4.5 months</span>
              </div>
              <div className="border-t border-gray-200 dark:border-gray-600 pt-3">
                <div className="flex justify-between items-center">
                  <span className="font-semibold text-gray-900 dark:text-white">12-Month ROI</span>
                  <span className="font-bold text-blue-600 dark:text-blue-400 text-lg">550%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Real-World Example */}
      <div className="bg-gradient-to-r from-indigo-50 to-blue-50 dark:from-gray-800 dark:to-gray-700 rounded-lg p-8">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
          Real-World Example: E-commerce Checkout
        </h2>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Imagine building an e-commerce checkout system that needs to integrate with multiple services:
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Without Service Virtualization</h3>
            <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
              <li className="flex items-center">
                <span className="w-2 h-2 bg-red-500 rounded-full mr-3"></span>
                Wait for 5 teams to provide their services
              </li>
              <li className="flex items-center">
                <span className="w-2 h-2 bg-red-500 rounded-full mr-3"></span>
                Coordinate test data across all systems
              </li>
              <li className="flex items-center">
                <span className="w-2 h-2 bg-red-500 rounded-full mr-3"></span>
                Debug issues across multiple systems
              </li>
              <li className="flex items-center">
                <span className="w-2 h-2 bg-red-500 rounded-full mr-3"></span>
                Limited test scenarios
              </li>
            </ul>
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-3">With Our Platform</h3>
            <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
              <li className="flex items-center">
                <span className="w-2 h-2 bg-green-500 rounded-full mr-3"></span>
                Create stubs for all 5 services immediately
              </li>
              <li className="flex items-center">
                <span className="w-2 h-2 bg-green-500 rounded-full mr-3"></span>
                Test hundreds of scenarios independently
              </li>
              <li className="flex items-center">
                <span className="w-2 h-2 bg-green-500 rounded-full mr-3"></span>
                Isolated debugging capabilities
              </li>
              <li className="flex items-center">
                <span className="w-2 h-2 bg-green-500 rounded-full mr-3"></span>
                Parallel development across teams
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* Architecture */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
          Platform Architecture
        </h2>
        <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-6 font-mono text-sm overflow-x-auto">
          <pre className="text-gray-800 dark:text-gray-200 whitespace-pre">
{`┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Web APIs  │    │  Messaging  │    │  Enterprise │
│             │    │             │    │             │
│ REST  SOAP  │    │ Kafka  AMQ  │    │ IBM   TIBCO │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
              ┌─────────────▼─────────────┐
              │  Service Virtualization  │
              │        Platform          │
              │                          │
              │ ✓ Stub Management        │
              │ ✓ Request Matching       │
              │ ✓ Response Generation    │
              │ ✓ Protocol Optimization  │
              │ ✓ Performance Monitoring │
              └──────────────────────────┘`}
          </pre>
        </div>
      </div>

      {/* Technical Specifications */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-8 border border-gray-200 dark:border-gray-700">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
          Technical Specifications
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-600 dark:text-blue-400 mb-2">10,000+</div>
            <div className="text-sm text-gray-600 dark:text-gray-400">Concurrent Connections</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-green-600 dark:text-green-400 mb-2">100K/sec</div>
            <div className="text-sm text-gray-600 dark:text-gray-400">Message Throughput</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-purple-600 dark:text-purple-400 mb-2">&lt;50ms</div>
            <div className="text-sm text-gray-600 dark:text-gray-400">Response Latency</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-orange-600 dark:text-orange-400 mb-2">99.9%</div>
            <div className="text-sm text-gray-600 dark:text-gray-400">Availability SLA</div>
          </div>
        </div>
      </div>

      {/* Strategic Impact */}
      <div className="bg-gradient-to-r from-green-50 to-emerald-50 dark:from-gray-800 dark:to-gray-700 rounded-lg p-8">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
          Strategic Impact
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Immediate Benefits</h3>
            <ul className="space-y-2 text-gray-600 dark:text-gray-300">
              <li>• 50% faster feature delivery</li>
              <li>• 75% reduction in integration defects</li>
              <li>• 90% improvement in developer productivity</li>
              <li>• $1.38M annual cost savings</li>
            </ul>
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Long-term Advantages</h3>
            <ul className="space-y-2 text-gray-600 dark:text-gray-300">
              <li>• Technology leadership position</li>
              <li>• Scalable foundation for growth</li>
              <li>• Innovation enablement capabilities</li>
              <li>• Organizational agility for market response</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="text-center py-8 border-t border-gray-200 dark:border-gray-700">
        <p className="text-gray-600 dark:text-gray-400">
          This platform represents a strategic enabler for our technology organization,
          providing both immediate cost savings and long-term competitive advantages.
        </p>
      </div>
    </div>
  );
};

export default About; 