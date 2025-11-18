const API_BASE = 'http://localhost:8081/api';
let currentUser = null;
let authToken = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('createUserForm')?.addEventListener('submit', handleCreateUser);
    document.getElementById('createProviderForm')?.addEventListener('submit', handleCreateProvider);
    document.getElementById('createInvoiceForm')?.addEventListener('submit', handleCreateInvoice);
    document.getElementById('createInvoiceFormSuper')?.addEventListener('submit', handleCreateInvoiceSuper);
    document.getElementById('updateInvoiceForm')?.addEventListener('submit', handleUpdateInvoice);
    document.getElementById('updateInvoiceFormSuper')?.addEventListener('submit', handleUpdateInvoiceSuper);
    document.getElementById('nlQueryForm')?.addEventListener('submit', handleNLQuery);
}

async function checkAuth() {
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('currentUser');

    if (token && user) {
        authToken = token;
        currentUser = JSON.parse(user);

        // Verify token is still valid by making a test request
        console.log('Checking if stored token is still valid for user:', currentUser.email);
        try {
            // Choose appropriate endpoint based on user role
            let testEndpoint = '/admin/users';
            if (currentUser.role === 'Customer') {
                testEndpoint = `/invoices/my-invoices?customerId=${currentUser.userId}`;
            } else if (currentUser.role === 'Invoice_Creator') {
                testEndpoint = `/invoices/my-created?creatorId=${currentUser.userId}`;
            } else if (currentUser.role === 'Super_Creator') {
                testEndpoint = `/invoices/provider?providerId=${currentUser.providerId}`;
            } else if (currentUser.role === 'Auditor') {
                testEndpoint = `/audit/invoices?auditorUserId=${currentUser.userId}`;
            }
            
            // Make a HEAD request to verify token without fetching data
            const response = await fetch(`${API_BASE}${testEndpoint}`, {
                method: 'HEAD',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            
            if (response.status === 401 || response.status === 403) {
                console.warn('Stored token is invalid or expired');
                localStorage.removeItem('authToken');
                localStorage.removeItem('currentUser');
                authToken = null;
                currentUser = null;
                alert('Your session has expired. Please login again.');
                showPage('loginPage');
                return;
            }
            
            console.log('Token is valid, showing dashboard');
            showDashboard();
        } catch (error) {
            console.error('Error checking token validity:', error);
            // If network error, still try to show dashboard
            showDashboard();
        }
    } else {
        console.log('No stored credentials found');
        showPage('loginPage');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Login failed');
        }

        const data = await response.json();
        console.log('Login response:', data);

        // Check if user needs to select a role
        if (data.requiresRoleSelection) {
            // User has multiple roles - show role selection page
            currentUser = data;
            showRoleSelection(data);
        } else {
            // User has single role or role already selected - proceed to dashboard
            authToken = data.token;
            currentUser = data;
            currentUser.role = data.selectedRole;  // Set current role

            console.log('Login successful! Token:', authToken);
            console.log('User data:', currentUser);

            localStorage.setItem('authToken', authToken);
            localStorage.setItem('currentUser', JSON.stringify(currentUser));

            showDashboard();
        }
    } catch (error) {
        document.getElementById('loginError').textContent = error.message;
    }
}

function logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    authToken = null;
    currentUser = null;
    showPage('loginPage');
}

function showPage(pageId) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');
}

function showDashboard() {
    showPage('dashboardPage');

    let userInfoText = `${currentUser.name} (${currentUser.role})`;
    if (currentUser.providerName) {
        userInfoText += ` - ${currentUser.providerName}`;
    }
    document.getElementById('userInfo').textContent = userInfoText;

    // Show role switcher if user has multiple roles
    if (currentUser.roles && currentUser.roles.length > 1) {
        document.getElementById('roleSwitcher').style.display = 'inline-block';
        setupRoleSwitcher();
    } else {
        document.getElementById('roleSwitcher').style.display = 'none';
    }

    // Hide all dashboards
    document.querySelectorAll('.dashboard').forEach(d => d.style.display = 'none');

    // Show appropriate dashboard
    switch(currentUser.role) {
        case 'Admin':
            document.getElementById('adminDashboard').style.display = 'block';
            loadUsers();
            loadProviders();
            break;
        case 'Customer':
            document.getElementById('customerDashboard').style.display = 'block';
            loadCustomerInvoices();
            break;
        case 'Invoice_Creator':
            document.getElementById('creatorDashboard').style.display = 'block';
            loadCreatorInvoices();
            break;
        case 'Super_Creator':
            document.getElementById('superCreatorDashboard').style.display = 'block';
            loadSuperCreatorInvoices();
            break;
        case 'Auditor':
            document.getElementById('auditorDashboard').style.display = 'block';
            loadAuditorInvoices();
            break;
    }
}

function showTab(tabName) {
    // Remove active from all tabs
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    // Add active to selected tab
    event.target.classList.add('active');
    document.getElementById(tabName + 'Tab').classList.add('active');

    // Load data for specific tabs
    if (tabName === 'logs') loadAuditLogs();
    if (tabName === 'pricing') loadPricingHistory();
}



async function apiCall(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json'
    };

    // Add JWT token if available
    if (authToken && authToken !== 'no-token') {
        headers['Authorization'] = `Bearer ${authToken}`;
        console.log('Making API call with token to:', endpoint);
    }
    else {
        console.log('Making API call without token to:', endpoint);
    }

    const mergedOptions = {
        ...options,
        headers: {
            ...headers,
            ...options.headers
        }
    };

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, mergedOptions);

        if (!response.ok) {
            if (response.status === 401) {
                // Session expired or invalid token - logout
                let errorMessage = 'Session expired. Please login again.';
                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                    console.error('Authentication error:', errorData);
                } catch (e) {
                    console.error('Could not parse error response');
                }
                
                // Unauthorized - redirect to login
                localStorage.removeItem('authToken');
                localStorage.removeItem('currentUser');
                authToken = null;
                currentUser = null;
                alert(errorMessage);
                showPage('loginPage');
                return null;

            } else if (response.status === 403) {
                // Permission denied - show error but don't logout
                let errorMessage = 'Access denied. You do not have permission to perform this action.';
                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                    console.error('Permission error:', errorData);
                } catch (e) {
                    console.error('Could not parse error response');
                }
                
                // Just show error message, don't logout
                throw new Error(errorMessage);
            }
            
            // Handle other errors
            let errorMessage = 'Request failed';
            try {
                const error = await response.json();
                errorMessage = error.message || errorMessage;
            } catch (e) {
                errorMessage = `Request failed with status ${response.status}`;
            }
            throw new Error(errorMessage);
        }

        return response.json();

    } catch (error) {
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            throw new Error('Network error. Please check your connection.');
        }
        throw error;
    }
}


// Admin Functions
async function handleCreateUser(e) {
    e.preventDefault();
    try {
        // Collect selected roles
        const selectedRoles = [];
        const roleCheckboxes = document.querySelectorAll('input[name="userRole"]:checked');
        roleCheckboxes.forEach(checkbox => {
            selectedRoles.push(checkbox.value);
        });

        const data = {
            providerId: parseInt(document.getElementById('userProviderId').value),
            name: document.getElementById('userName').value,
            email: document.getElementById('userEmail').value,
            address: document.getElementById('userAddress').value,
            password: document.getElementById('userPassword').value,
            phoneNumber: document.getElementById('userPhone').value,
            roles: selectedRoles.length > 0 ? selectedRoles : null
        };

        await apiCall('/admin/users', { method: 'POST', body: JSON.stringify(data) });
        alert('User created successfully!');
        e.target.reset();
        loadUsers();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function loadUsers() {
    try {
        const users = await apiCall('/admin/users');
        if (!users) return; // Token expired, user redirected to login
        const html = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Role</th>
                            <th>Provider</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${users.map(u => `
                            <tr>
                                <td>${u.userId}</td>
                                <td>${u.name}</td>
                                <td>${u.email}</td>
                                <td><span class="badge badge-info">${u.role}</span></td>
                                <td>${u.provider?.name || 'No Provider'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        document.getElementById('usersList').innerHTML = html;
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

async function handleCreateProvider(e) {
    e.preventDefault();
    try {
        const data = {
            name: document.getElementById('providerName').value,
            city: document.getElementById('providerCity').value,
            email: document.getElementById('providerEmail').value,
            phoneNumber: document.getElementById('providerPhone').value,
            currentKwhPrice: parseFloat(document.getElementById('providerPrice').value)
        };

        await apiCall('/admin/providers', { method: 'POST', body: JSON.stringify(data) });
        alert('Provider created successfully!');
        e.target.reset();
        loadProviders();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function loadProviders() {
    try {
        // Get only the provider that the admin belongs to
        if (!currentUser.providerId) {
            document.getElementById('providersList').innerHTML = '<p>No provider assigned to this admin.</p>';
            return;
        }

        const provider = await apiCall(`/admin/providers/${currentUser.providerId}`);
        if (!provider) return; // Token expired, user redirected to login
        const html = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>City</th>
                            <th>Email</th>
                            <th>Current kWh Price</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>${provider.providerId}</td>
                            <td>${provider.name}</td>
                            <td>${provider.city}</td>
                            <td>${provider.email}</td>
                            <td>${provider.currentKwhPrice}</td>
                            <td>
                                <button class="btn btn-primary" onclick="updatePrice(${provider.providerId})">Update Price</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `;
        document.getElementById('providersList').innerHTML = html;
    } catch (error) {
        console.error('Error loading providers:', error);
        document.getElementById('providersList').innerHTML = '<p style="color: red;">Error loading provider information.</p>';
    }
}

async function updatePrice(providerId) {
    const newPrice = prompt('Enter new kWh price:');
    if (newPrice) {
        try {
            await apiCall(`/admin/providers/${providerId}/price`, {
                method: 'PUT',
                body: JSON.stringify({ newKwhPrice: parseFloat(newPrice) })
            });
            alert('Price updated successfully!');
            loadProviders();
        } catch (error) {
            alert('Error: ' + error.message);
        }
    }
}

// Invoice Functions
async function handleCreateInvoice(e) {
    e.preventDefault();
    try {
        const data = {
            customerId: parseInt(document.getElementById('invoiceCustomerId').value),
            kwhConsumed: parseFloat(document.getElementById('invoiceKwh').value),
            issueDate: document.getElementById('invoiceIssueDate').value,
            dueDate: document.getElementById('invoiceDueDate').value
        };

        await apiCall(`/invoices?creatorUserId=${currentUser.userId}`, { method: 'POST', body: JSON.stringify(data) });
        alert('Invoice created successfully!');
        e.target.reset();
        loadCreatorInvoices();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function handleCreateInvoiceSuper(e) {
    e.preventDefault();
    try {
        const data = {
            customerId: parseInt(document.getElementById('invoiceCustomerIdSuper').value),
            kwhConsumed: parseFloat(document.getElementById('invoiceKwhSuper').value),
            issueDate: document.getElementById('invoiceIssueDateSuper').value,
            dueDate: document.getElementById('invoiceDueDateSuper').value
        };

        await apiCall(`/invoices?creatorUserId=${currentUser.userId}`, { method: 'POST', body: JSON.stringify(data) });
        alert('Invoice created successfully!');
        e.target.reset();
        loadSuperCreatorInvoices();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function loadCustomerInvoices() {
    try {
        console.log('Loading customer invoices for userId:', currentUser.userId);
        const invoices = await apiCall(`/invoices/my-invoices?customerId=${currentUser.userId}`);
        if (!invoices) return; // Token expired, user redirected to login
        console.log('Received invoices:', invoices);
        displayInvoices(invoices, 'customerInvoices');
    } catch (error) {
        console.error('Error loading invoices:', error);
        document.getElementById('customerInvoices').innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
    }
}

async function loadCreatorInvoices() {
    try {
        const invoices = await apiCall(`/invoices/my-created?creatorId=${currentUser.userId}`);
        if (!invoices) return; // Token expired, user redirected to login
        displayInvoices(invoices, 'creatorInvoices');
    } catch (error) {
        console.error('Error loading invoices:', error);
    }
}

async function loadSuperCreatorInvoices() {
    try {
        const invoices = await apiCall(`/invoices/provider?providerId=${currentUser.providerId}`);
        if (!invoices) return; // Token expired, user redirected to login
        displayInvoices(invoices, 'superCreatorInvoices');
    } catch (error) {
        console.error('Error loading invoices:', error);
    }
}

async function loadAuditorInvoices() {
    try {
        const invoices = await apiCall(`/audit/invoices?auditorUserId=${currentUser.userId}`);
        if (!invoices) return; // Token expired, user redirected to login
        displayInvoices(invoices, 'auditorInvoices');
    } catch (error) {
        console.error('Error loading invoices:', error);
    }
}

async function searchAuditorInvoices() {
    const invoiceNumber = document.getElementById('auditorInvoiceSearchInput').value.trim();
    
    if (!invoiceNumber) {
        alert('Please enter an Invoice Number');
        return;
    }

    try {
        const invoices = await apiCall(`/audit/invoices/search?invoiceNumber=${encodeURIComponent(invoiceNumber)}&auditorUserId=${currentUser.userId}`);
        if (!invoices) return; // Token expired, user redirected to login
        
        if (invoices.length === 0) {
            document.getElementById('auditorInvoices').innerHTML = `
                <div style="text-align: center; padding: 40px; color: #666;">
                    <p style="font-size: 16px;">No invoice found with number: <strong>${invoiceNumber}</strong></p>
                    <p style="font-size: 14px; color: #999;">Please check the invoice number and try again</p>
                </div>
            `;
            return;
        }

        displayInvoices(invoices, 'auditorInvoices');
    } catch (error) {
        console.error('Error searching invoices:', error);
        document.getElementById('auditorInvoices').innerHTML = `
            <div style="text-align: center; padding: 40px; color: #d32f2f;">
                <p style="font-size: 16px;">Error searching invoices</p>
                <p style="font-size: 14px;">${error.message || 'Please try again'}</p>
            </div>
        `;
    }
}

function clearAuditorInvoiceSearch() {
    document.getElementById('auditorInvoiceSearchInput').value = '';
    loadAuditorInvoices();
}

function displayInvoices(invoices, containerId) {
    console.log('Displaying invoices:', invoices); // Debug log

    if (!invoices || invoices.length === 0) {
        document.getElementById(containerId).innerHTML = '<p>No invoices found.</p>';
        return;
    }

    const statusBadge = (status) => {
        const badges = {
            'Paid': 'badge-success',
            'Pending': 'badge-warning',
            'Overdue': 'badge-danger',
            'Cancelled': 'badge-info'
        };
        return `<span class="badge ${badges[status]}">${status}</span>`;
    };

    // Check if user can edit (Invoice_Creator or Super_Creator)
    const canEdit = currentUser && (currentUser.role === 'Invoice_Creator' || currentUser.role === 'Super_Creator');

    const html = `
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Invoice #</th>
                        <th>Customer</th>
                        <th>Provider</th>
                        <th>Created By</th>
                        <th>kWh</th>
                        <th>Price/kWh</th>
                        <th>Total Amount</th>
                        <th>Issue Date</th>
                        <th>Due Date</th>
                        <th>Payment Date</th>
                        <th>Status</th>
                        ${canEdit ? '<th>Actions</th>' : ''}
                    </tr>
                </thead>
                <tbody>
                    ${invoices.map(inv => `
                        <tr>
                            <td>${inv.invoiceNumber || 'N/A'}</td>
                            <td>${inv.customer?.name || 'N/A'}</td>
                            <td>${inv.provider?.name || 'N/A'}</td>
                            <td>${inv.createdByUser?.name || 'N/A'}</td>
                            <td>${inv.kwhConsumed || 0}</td>
                            <td>${inv.pricing?.kwhPrice || 'N/A'}</td>
                            <td>${inv.totalAmount || 0}</td>
                            <td>${inv.issueDate || 'N/A'}</td>
                            <td>${inv.dueDate || 'N/A'}</td>
                            <td>${inv.paymentDate || '-'}</td>
                            <td>${statusBadge(inv.paymentStatus)}</td>
                            ${canEdit ? `<td><button class="btn btn-sm btn-primary" onclick='openUpdateModal(${JSON.stringify(inv)})'>Edit</button></td>` : ''}
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
    document.getElementById(containerId).innerHTML = html;
}

// Audit Functions
async function loadAuditLogs() {
    // Show initial message instead of loading all logs
    document.getElementById('auditLogs').innerHTML = `
        <div style="text-align: center; padding: 40px; color: #666;">
            <p style="font-size: 16px;">Enter an Invoice Number above to search for audit logs</p>
        </div>
    `;
}

async function searchAuditLogs() {
    const invoiceNumber = document.getElementById('invoiceSearchInput').value.trim();
    
    if (!invoiceNumber) {
        alert('Please enter an Invoice Number');
        return;
    }

    try {
        const logs = await apiCall(`/audit/logs/search?invoiceNumber=${encodeURIComponent(invoiceNumber)}&auditorUserId=${currentUser.userId}`);
        if (!logs) return; // Token expired, user redirected to login
        
        if (logs.length === 0) {
            document.getElementById('auditLogs').innerHTML = `
                <div style="text-align: center; padding: 40px; color: #666;">
                    <p style="font-size: 16px;">No audit logs found for Invoice Number: <strong>${invoiceNumber}</strong></p>
                    <p style="font-size: 14px; color: #999;">Please check the invoice number and try again</p>
                </div>
            `;
            return;
        }

        const html = `
            <div style="margin-bottom: 15px; padding: 10px; background-color: #e8f5e9; border-left: 4px solid #4caf50; border-radius: 4px;">
                <strong>Found ${logs.length} audit log(s) for Invoice: ${invoiceNumber}</strong>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Invoice #</th>
                            <th>Action</th>
                            <th>Performed By</th>
                            <th>Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${logs.map(log => `
                            <tr>
                                <td>${log.auditId}</td>
                                <td>${log.invoice?.invoiceNumber || 'N/A'}</td>
                                <td><span class="badge badge-info">${log.action}</span></td>
                                <td>${log.performedByUser?.name || 'N/A'}</td>
                                <td>${new Date(log.performedAt).toLocaleString()}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        document.getElementById('auditLogs').innerHTML = html;
    } catch (error) {
        console.error('Error searching audit logs:', error);
        document.getElementById('auditLogs').innerHTML = `
            <div style="text-align: center; padding: 40px; color: #d32f2f;">
                <p style="font-size: 16px;">Error searching audit logs</p>
                <p style="font-size: 14px;">${error.message || 'Please try again'}</p>
            </div>
        `;
    }
}

function clearAuditSearch() {
    document.getElementById('invoiceSearchInput').value = '';
    loadAuditLogs();
}

// Helper function to format cell values nicely
function formatCellValue(val) {
    if (val === null || val === undefined) {
        return '<span style="color: #999; font-style: italic;">null</span>';
    }
    
    // Check if it's a JSON string
    if (typeof val === 'string' && (val.startsWith('{') || val.startsWith('['))) {
        try {
            const parsed = JSON.parse(val);
            
            // If it's an array, format differently
            if (Array.isArray(parsed)) {
                return parsed.map(item => 
                    `<div style="margin: 2px 0; padding: 4px 8px; background: #f8f9fa; border-radius: 3px;">${item}</div>`
                ).join('');
            }
            
            // Format object as vertical list with better styling
            return Object.entries(parsed).map(([key, value]) => {
                // Format the key nicely (camelCase to Title Case)
                const formattedKey = key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
                return `<div style="margin: 4px 0; padding: 3px 0; border-bottom: 1px dotted #e0e0e0;">
                    <strong style="color: #667eea;">${formattedKey}:</strong> 
                    <span style="color: #2c3e50;">${value !== null ? value : '<i style="color: #999;">null</i>'}</span>
                </div>`;
            }).join('');
        } catch (e) {
            // If parsing fails, return as is
            return val;
        }
    }
    
    return val;
}

async function handleNLQuery(e) {
    e.preventDefault();
    const query = document.getElementById('nlQuery').value;

    try {
        const result = await apiCall(`/audit/query?auditorUserId=${currentUser.userId}`, {
            method: 'POST',
            body: JSON.stringify({ query })
        });

        const html = `
            <div class="query-result">
                <h4>Query: ${result.query}</h4>
                <p><strong>Generated SQL:</strong></p>
                <pre>${result.generatedSQL}</pre>
                <p><strong>Results (${result.rowCount} rows):</strong></p>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                ${result.results.length > 0 ? Object.keys(result.results[0]).map(key => `<th>${key}</th>`).join('') : ''}
                            </tr>
                        </thead>
                        <tbody>
                            ${result.results.map(row => `
                                <tr>
                                    ${Object.values(row).map(val => `<td>${formatCellValue(val)}</td>`).join('')}
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        document.getElementById('queryResults').innerHTML = html;
    } catch (error) {
        document.getElementById('queryResults').innerHTML = `
            <div class="error">Error: ${error.message}</div>
        `;
    }
}

// Update Invoice Functions
function openUpdateModal(invoice) {
    const modalId = currentUser.role === 'Super_Creator' ? 'updateInvoiceModalSuper' : 'updateInvoiceModal';
    const prefix = currentUser.role === 'Super_Creator' ? 'Super' : '';

    document.getElementById(`updateInvoiceId${prefix}`).value = invoice.invoiceId;
    document.getElementById(`updateInvoiceNumber${prefix}`).textContent = invoice.invoiceNumber;
    document.getElementById(`updateKwh${prefix}`).value = invoice.kwhConsumed;
    document.getElementById(`updateDueDate${prefix}`).value = invoice.dueDate;
    document.getElementById(`updatePaymentStatus${prefix}`).value = invoice.paymentStatus;
    document.getElementById(`updatePaymentDate${prefix}`).value = invoice.paymentDate || '';

    document.getElementById(modalId).style.display = 'block';
}

function closeUpdateModal() {
    document.getElementById('updateInvoiceModal').style.display = 'none';
}

function closeUpdateModalSuper() {
    document.getElementById('updateInvoiceModalSuper').style.display = 'none';
}

async function handleUpdateInvoice(e) {
    e.preventDefault();

    try {
        const invoiceId = document.getElementById('updateInvoiceId').value;
        const data = {};

        const kwh = document.getElementById('updateKwh').value;
        const dueDate = document.getElementById('updateDueDate').value;
        const paymentStatus = document.getElementById('updatePaymentStatus').value;
        const paymentDate = document.getElementById('updatePaymentDate').value;

        if (kwh) data.kwhConsumed = parseFloat(kwh);
        if (dueDate) data.dueDate = dueDate;
        if (paymentStatus) data.paymentStatus = paymentStatus;
        if (paymentDate) data.paymentDate = paymentDate;

        await apiCall(`/invoices/${invoiceId}?updaterUserId=${currentUser.userId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        alert('Invoice updated successfully!');
        closeUpdateModal();
        loadCreatorInvoices();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function handleUpdateInvoiceSuper(e) {
    e.preventDefault();

    try {
        const invoiceId = document.getElementById('updateInvoiceIdSuper').value;
        const data = {};

        const kwh = document.getElementById('updateKwhSuper').value;
        const dueDate = document.getElementById('updateDueDateSuper').value;
        const paymentStatus = document.getElementById('updatePaymentStatusSuper').value;
        const paymentDate = document.getElementById('updatePaymentDateSuper').value;

        if (kwh) data.kwhConsumed = parseFloat(kwh);
        if (dueDate) data.dueDate = dueDate;
        if (paymentStatus) data.paymentStatus = paymentStatus;
        if (paymentDate) data.paymentDate = paymentDate;

        await apiCall(`/invoices/${invoiceId}?updaterUserId=${currentUser.userId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        alert('Invoice updated successfully!');
        closeUpdateModalSuper();
        loadSuperCreatorInvoices();
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Pricing History Functions
async function loadPricingHistory() {
    try {
        const history = await apiCall(`/audit/pricing-history?auditorUserId=${currentUser.userId}`);
        if (!history) return; // Token expired, user redirected to login
        displayPricingHistory(history);
    } catch (error) {
        console.error('Error loading pricing history:', error);
        document.getElementById('pricingHistory').innerHTML = '<p class="error">Error loading pricing history: ' + error.message + '</p>';
    }
}

function displayPricingHistory(history) {
    if (!history || history.length === 0) {
        document.getElementById('pricingHistory').innerHTML = '<p>No pricing history found.</p>';
        return;
    }

    const html = `
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Changed By (Admin)</th>
                        <th>Price (₪/kWh)</th>
                        <th>Valid From</th>
                        <th>Valid To</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${history.map(h => `
                        <tr>
                            <td><strong>${h.changedByUser?.name || 'N/A'}</strong></td>
                            <td>${h.kwhPrice}</td>
                            <td>${new Date(h.validFrom).toLocaleString()}</td>
                            <td>${h.validTo ? new Date(h.validTo).toLocaleString() : '<span class="badge badge-success">Current</span>'}</td>
                            <td>${h.validTo ? '<span class="badge badge-info">Expired</span>' : '<span class="badge badge-success">Active ✓</span>'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
    document.getElementById('pricingHistory').innerHTML = html;
}

// ========== Multi-Role Functions ==========

// Role icons mapping
const roleIcons = {
    'Admin': '👑',
    'Customer': '👤',
    'Invoice_Creator': '📝',
    'Super_Creator': '⚡',
    'Auditor': '🔍'
};

// Role descriptions
const roleDescriptions = {
    'Admin': 'Manage users, providers, and system settings',
    'Customer': 'View and manage your invoices',
    'Invoice_Creator': 'Create and manage invoices',
    'Super_Creator': 'Full provider invoice management',
    'Auditor': 'Audit and review system data'
};

function showRoleSelection(userData) {
    showPage('roleSelectionPage');
    
    document.getElementById('roleSelectionUserName').textContent = userData.name;
    
    const roleButtonsHtml = userData.roles.map(role => `
        <button class="role-button" onclick="selectRole('${role}', ${userData.userId})">
            <span class="role-icon">${roleIcons[role] || '🔹'}</span>
            <div class="role-text">
                <span class="role-name">Continue as ${role.replace('_', ' ')}</span>
                <span class="role-description">${roleDescriptions[role] || ''}</span>
            </div>
        </button>
    `).join('');
    
    document.getElementById('roleButtons').innerHTML = roleButtonsHtml;
}

async function selectRole(selectedRole, userId) {
    try {
        const response = await fetch(`${API_BASE}/auth/select-role?userId=${userId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ selectedRole })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Role selection failed');
        }

        const data = await response.json();
        authToken = data.token;
        currentUser = data;
        currentUser.role = data.selectedRole;

        console.log('Role selected:', selectedRole);
        console.log('New token:', authToken);

        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));

        showDashboard();
    } catch (error) {
        alert('Error selecting role: ' + error.message);
    }
}

function setupRoleSwitcher() {
    const dropdown = document.getElementById('roleSwitcherDropdown');
    
    const rolesHtml = currentUser.roles.map(role => `
        <div class="role-switcher-item ${role === currentUser.role ? 'current' : ''}" 
             onclick="switchRole('${role}')">
            <span class="role-icon">${roleIcons[role] || '🔹'}</span>
            <span>${role.replace('_', ' ')}${role === currentUser.role ? ' (Current)' : ''}</span>
        </div>
    `).join('');
    
    dropdown.innerHTML = rolesHtml;
}

function toggleRoleSwitcher() {
    const dropdown = document.getElementById('roleSwitcherDropdown');
    dropdown.classList.toggle('active');
}

// Close dropdown when clicking outside
document.addEventListener('click', function(event) {
    const switcher = document.getElementById('roleSwitcher');
    const dropdown = document.getElementById('roleSwitcherDropdown');
    
    if (switcher && !switcher.contains(event.target)) {
        dropdown?.classList.remove('active');
    }
});

async function switchRole(newRole) {
    if (newRole === currentUser.role) {
        document.getElementById('roleSwitcherDropdown').classList.remove('active');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/auth/switch-role?userId=${currentUser.userId}`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({ selectedRole: newRole })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Role switch failed');
        }

        const data = await response.json();
        authToken = data.token;
        currentUser = data;
        currentUser.role = data.selectedRole;

        console.log('Switched to role:', newRole);
        console.log('New token:', authToken);

        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));

        // Close dropdown
        document.getElementById('roleSwitcherDropdown').classList.remove('active');

        // Reload dashboard with new role
        showDashboard();
    } catch (error) {
        alert('Error switching role: ' + error.message);
    }
}
