document.addEventListener('DOMContentLoaded', () => {
  initLogin();
  initCalendar();
  fetchWeather();
  initWeatherSearch();
});

const LOGIN_SESSION_KEY = 'ecai_active_user';
const LOGIN_LAST_ACTIVE_KEY = 'ecai_last_active_at';
const LOGIN_IDLE_LIMIT_MS = 5 * 60 * 1000;
let idleTimeoutId = null;
let idleListenersAttached = false;

/* =========================================================
   LOGIN LOGIC
========================================================= */
function initLogin() {
  const loginBtn = document.getElementById('login-btn');
  const userInp = document.getElementById('login-username');
  const passInp = document.getElementById('login-password');
  const errTxt = document.getElementById('login-error');
  const loginSec = document.getElementById('login-section');
  const dashSec = document.getElementById('main-dashboard');
  
  if (!loginBtn) return;

  const validUsers = ['kiran', 'vaishnavi', 'krishna', 'kishore', 'bhargav', 'jaswanth', 'vamsi', 'prathima'];
  const applyLoggedInState = () => {
     const activeUser = getStoredActiveUser();
     errTxt.style.display = 'none';
     loginSec.style.display = 'none';
     dashSec.style.display = 'grid';
     updateUserMenuUI(activeUser);
     updateWelcomeUsername(activeUser);
  };
  const applyLoggedOutState = () => {
     loginSec.style.display = 'flex';
     dashSec.style.display = 'none';
     passInp.value = '';
     closeUserMenu();
     updateUserMenuUI('');
     updateWelcomeUsername('');
  };
  const performLogout = () => {
     clearLoginSession();
     applyLoggedOutState();
  };

  initUserMenu(performLogout);

  const handleLogin = () => {
     const normalizedUser = userInp.value.trim().toLowerCase();
     if (validUsers.includes(normalizedUser) && passInp.value === '1234') {
        persistLoginSession(normalizedUser);
        applyLoggedInState();
        startIdleSessionWatch(() => {
           performLogout();
        });
     } else {
        errTxt.style.display = 'block';
     }
  };

  const activeUser = getStoredActiveUser();
  if (activeUser) {
     userInp.value = activeUser;
     applyLoggedInState();
     startIdleSessionWatch(() => {
        performLogout();
     });
  } else {
     clearLoginSession();
     applyLoggedOutState();
  }

  loginBtn.addEventListener('click', handleLogin);
  passInp.addEventListener('keypress', (e) => {
     if (e.key === 'Enter') handleLogin();
  });
  userInp.addEventListener('keypress', (e) => {
     if (e.key === 'Enter') handleLogin();
  });
}

function persistLoginSession(username) {
  const now = Date.now();
  localStorage.setItem(LOGIN_SESSION_KEY, username);
  localStorage.setItem(LOGIN_LAST_ACTIVE_KEY, String(now));
}

function clearLoginSession() {
  localStorage.removeItem(LOGIN_SESSION_KEY);
  localStorage.removeItem(LOGIN_LAST_ACTIVE_KEY);
  clearIdleSessionWatch();
}

function getStoredActiveUser() {
  const username = localStorage.getItem(LOGIN_SESSION_KEY);
  const lastActiveRaw = localStorage.getItem(LOGIN_LAST_ACTIVE_KEY);

  if (!username || !lastActiveRaw) {
    return '';
  }

  const lastActive = Number(lastActiveRaw);
  if (!Number.isFinite(lastActive) || Date.now() - lastActive > LOGIN_IDLE_LIMIT_MS) {
    return '';
  }

  return username;
}

function startIdleSessionWatch(onTimeout) {
  const activityEvents = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'];
  const markActivity = () => {
    if (!localStorage.getItem(LOGIN_SESSION_KEY)) return;

    localStorage.setItem(LOGIN_LAST_ACTIVE_KEY, String(Date.now()));
    scheduleIdleLogout(onTimeout);
  };

  if (!idleListenersAttached) {
    activityEvents.forEach((eventName) => {
      window.addEventListener(eventName, markActivity, { passive: true });
    });
    idleListenersAttached = true;
  }

  scheduleIdleLogout(onTimeout);
}

function scheduleIdleLogout(onTimeout) {
  if (idleTimeoutId) {
    window.clearTimeout(idleTimeoutId);
  }

  const lastActive = Number(localStorage.getItem(LOGIN_LAST_ACTIVE_KEY) || Date.now());
  const timeRemaining = Math.max(0, LOGIN_IDLE_LIMIT_MS - (Date.now() - lastActive));

  idleTimeoutId = window.setTimeout(() => {
    onTimeout();
  }, timeRemaining);
}

function clearIdleSessionWatch() {
  if (idleTimeoutId) {
    window.clearTimeout(idleTimeoutId);
    idleTimeoutId = null;
  }
}

function initUserMenu(onLogout) {
  const trigger = document.getElementById('user-menu-trigger');
  const menu = document.getElementById('user-menu');
  const logoutBtn = document.getElementById('logout-btn');

  if (!trigger || !menu || !logoutBtn || trigger.dataset.bound === 'true') {
    return;
  }

  trigger.dataset.bound = 'true';

  trigger.addEventListener('click', (event) => {
    event.stopPropagation();
    const isOpen = !menu.hasAttribute('hidden');
    if (isOpen) {
      closeUserMenu();
    } else {
      menu.removeAttribute('hidden');
      trigger.setAttribute('aria-expanded', 'true');
    }
  });

  logoutBtn.addEventListener('click', () => {
    onLogout();
  });

  document.addEventListener('click', (event) => {
    if (!menu.hasAttribute('hidden') && !event.target.closest('.user-profile')) {
      closeUserMenu();
    }
  });
}

function updateUserMenuUI(username) {
  const trigger = document.getElementById('user-menu-trigger');
  if (!trigger) return;

  if (!username) {
    trigger.hidden = true;
    trigger.textContent = 'U';
    trigger.setAttribute('aria-label', 'Open user menu');
    return;
  }

  trigger.hidden = false;
  trigger.textContent = username.charAt(0).toUpperCase();
  trigger.setAttribute('aria-label', `${username} menu`);
}

function closeUserMenu() {
  const trigger = document.getElementById('user-menu-trigger');
  const menu = document.getElementById('user-menu');

  if (menu) {
    menu.setAttribute('hidden', '');
  }

  if (trigger) {
    trigger.setAttribute('aria-expanded', 'false');
  }
}

function updateWelcomeUsername(username) {
  const welcomeUsername = document.getElementById('welcome-username');
  if (!welcomeUsername) return;

  welcomeUsername.textContent = username ? formatDisplayName(username) : 'Employee';
}

function formatDisplayName(username) {
  return username
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ');
}

/* =========================================================
   CALENDAR LOGIC
======================================================== */
let currentDate = new Date();
let selectedDate = new Date();
let isYearViewOpen = false;
const MONTH_NAMES = ["January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"];
const INDIA_PUBLIC_HOLIDAYS = {
  2026: {
    '2026-01-26': 'Republic Day',
    '2026-03-03': 'Holi',
    '2026-03-21': 'Idul Fitr',
    '2026-03-27': 'Ram Navami',
    '2026-04-03': 'Good Friday',
    '2026-05-01': 'Buddha Purnima',
    '2026-05-27': 'Bakrid',
    '2026-06-26': 'Muharram',
    '2026-08-15': 'Independence Day',
    '2026-09-04': 'Janmashtami',
    '2026-10-02': 'Gandhi Jayanti',
    '2026-10-21': 'Vijaya Dashami',
    '2026-11-08': 'Diwali',
    '2026-11-24': 'Guru Nanak Jayanti',
    '2026-12-25': 'Christmas Day'
  }
};

function initCalendar() {
  const prevBtn = document.getElementById('prev-month');
  const nextBtn = document.getElementById('next-month');
  const currentMonthYearElement = document.getElementById('current-month-year');
  const launcherIds = ['btn-open-teams', 'btn-open-codex', 'btn-open-antigravity', 'btn-open-outlook'];

  if (!prevBtn || !nextBtn || !currentMonthYearElement) return;

  launcherIds.forEach((id) => {
    const launcher = document.getElementById(id);
    if (launcher) {
      launcher.setAttribute('target', '_self');
      launcher.setAttribute('rel', 'noopener noreferrer');
    }
  });

  prevBtn.addEventListener('click', () => {
    if (isYearViewOpen) {
      currentDate.setFullYear(currentDate.getFullYear() - 1);
    } else {
      currentDate.setMonth(currentDate.getMonth() - 1);
    }
    renderCalendar();
  });
  
  nextBtn.addEventListener('click', () => {
    if (isYearViewOpen) {
      currentDate.setFullYear(currentDate.getFullYear() + 1);
    } else {
      currentDate.setMonth(currentDate.getMonth() + 1);
    }
    renderCalendar();
  });

  currentMonthYearElement.addEventListener('click', () => {
    isYearViewOpen = !isYearViewOpen;
    renderCalendar();
  });

  renderCalendar();
}

function renderCalendar() {
  const currentMonthYearElement = document.getElementById('current-month-year');
  const daysGrid = document.getElementById('days-grid');
  const weekdays = document.getElementById('calendar-weekdays');
  
  if (!currentMonthYearElement || !daysGrid || !weekdays) return;

  daysGrid.innerHTML = '';
  
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  currentMonthYearElement.textContent = isYearViewOpen ? `${year}` : `${MONTH_NAMES[month]} ${year}`;
  currentMonthYearElement.setAttribute('aria-expanded', String(isYearViewOpen));

  if (isYearViewOpen) {
    weekdays.style.display = 'none';
    daysGrid.className = 'year-grid';
    renderYearView(daysGrid, year);
    return;
  }

  weekdays.style.display = 'grid';
  daysGrid.className = 'days-grid';

  const firstDayOfMonth = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const daysInPrevMonth = new Date(year, month, 0).getDate();
  const realToday = new Date();
  const isActualMonthAndYear = realToday.getMonth() === month && realToday.getFullYear() === year;

  for (let i = firstDayOfMonth; i > 0; i--) {
    const dayCell = document.createElement('div');
    dayCell.classList.add('day-cell', 'other-month');
    const dayDate = daysInPrevMonth - i + 1;
    dayCell.textContent = dayDate;
    
    const dayOfWeek = new Date(year, month - 1, dayDate).getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      dayCell.classList.add('weekend');
    }

    applyHolidayState(dayCell, new Date(year, month - 1, dayDate));
    dayCell.addEventListener('click', () => handleDayClick(new Date(year, month - 1, dayDate)));
    daysGrid.appendChild(dayCell);
  }

  for (let i = 1; i <= daysInMonth; i++) {
    const dayCell = document.createElement('div');
    dayCell.classList.add('day-cell');
    dayCell.textContent = i;
    
    const dayOfWeek = new Date(year, month, i).getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      dayCell.classList.add('weekend');
    }
    
    if (isActualMonthAndYear && i === realToday.getDate()) {
      dayCell.classList.add('today');
    }
    
    const cellDate = new Date(year, month, i);
    if (selectedDate && cellDate.toDateString() === selectedDate.toDateString()) {
       dayCell.classList.add('active-day');
    }

    applyHolidayState(dayCell, cellDate);
    dayCell.addEventListener('click', () => handleDayClick(cellDate));
    daysGrid.appendChild(dayCell);
  }

  const totalRenderedCells = firstDayOfMonth + daysInMonth;
  const rowCount = Math.ceil(totalRenderedCells / 7);
  const totalGridCells = rowCount * 7;
  const nextMonthCells = totalGridCells - totalRenderedCells;

  for (let i = 1; i <= nextMonthCells; i++) {
    const dayCell = document.createElement('div');
    dayCell.classList.add('day-cell', 'other-month');
    dayCell.textContent = i;
    
    const dayOfWeek = new Date(year, month + 1, i).getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      dayCell.classList.add('weekend');
    }

    applyHolidayState(dayCell, new Date(year, month + 1, i));
    dayCell.addEventListener('click', () => handleDayClick(new Date(year, month + 1, i)));
    daysGrid.appendChild(dayCell);
  }
}

function renderYearView(container, year) {
  MONTH_NAMES.forEach((monthName, monthIndex) => {
    const monthCard = document.createElement('div');
    monthCard.className = 'month-card';

    if (currentDate.getFullYear() === year && currentDate.getMonth() === monthIndex) {
      monthCard.classList.add('active-month-card');
    }

    if (selectedDate.getFullYear() === year && selectedDate.getMonth() === monthIndex) {
      monthCard.classList.add('selected-month-card');
    }

    const headerButton = document.createElement('button');
    headerButton.type = 'button';
    headerButton.className = 'month-card-header';
    headerButton.innerHTML = `
      <span class="month-card-name">${monthName}</span>
      <span class="month-card-meta">${year}</span>
    `;
    headerButton.addEventListener('click', () => {
      currentDate = new Date(year, monthIndex, 1);
      isYearViewOpen = false;
      renderCalendar();
    });

    const weekdayRow = document.createElement('div');
    weekdayRow.className = 'mini-weekdays';
    ['S', 'M', 'T', 'W', 'T', 'F', 'S'].forEach((label) => {
      const weekday = document.createElement('span');
      weekday.textContent = label;
      weekdayRow.appendChild(weekday);
    });

    const datesGrid = document.createElement('div');
    datesGrid.className = 'mini-days-grid';
    const totalDays = new Date(year, monthIndex + 1, 0).getDate();
    const firstDay = new Date(year, monthIndex, 1).getDay();

    for (let i = 0; i < firstDay; i++) {
      const spacer = document.createElement('span');
      spacer.className = 'mini-day mini-day-empty';
      datesGrid.appendChild(spacer);
    }

    for (let day = 1; day <= totalDays; day++) {
      const cellDate = new Date(year, monthIndex, day);
      const dateButton = document.createElement('button');
      dateButton.type = 'button';
      dateButton.className = 'mini-day';
      dateButton.textContent = String(day);

      if (cellDate.toDateString() === new Date().toDateString()) {
        dateButton.classList.add('mini-day-today');
      }

      if (selectedDate && cellDate.toDateString() === selectedDate.toDateString()) {
        dateButton.classList.add('mini-day-selected');
      }

      if (cellDate.getDay() === 0 || cellDate.getDay() === 6) {
        dateButton.classList.add('mini-day-weekend');
      }

      applyHolidayState(dateButton, cellDate, 'mini');

      dateButton.addEventListener('click', () => {
        selectedDate = cellDate;
        currentDate = new Date(cellDate);
        isYearViewOpen = false;
        renderCalendar();
      });

      datesGrid.appendChild(dateButton);
    }

    monthCard.appendChild(headerButton);
    monthCard.appendChild(weekdayRow);
    monthCard.appendChild(datesGrid);
    container.appendChild(monthCard);
  });
}

function handleDayClick(date) {
   selectedDate = date;
   if(currentDate.getMonth() !== date.getMonth() || currentDate.getFullYear() !== date.getFullYear()) {
      currentDate = new Date(date);
   }
   renderCalendar();
}

function applyHolidayState(element, date, variant = 'default') {
  const holidayName = getHolidayName(date);
  if (!holidayName) return;

  element.classList.add(variant === 'mini' ? 'mini-day-holiday' : 'public-holiday');
  element.setAttribute('data-tooltip', holidayName);
  element.setAttribute('aria-label', `${date.toDateString()} - ${holidayName}`);
}

function getHolidayName(date) {
  const yearHolidays = INDIA_PUBLIC_HOLIDAYS[date.getFullYear()];
  if (!yearHolidays) return '';
  return yearHolidays[formatDateKey(date)] || '';
}

function formatDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/* =========================================================
   WEATHER LOGIC
========================================================= */
const WEATHER_API_BASE = "https://api.open-meteo.com/v1/forecast";
let defaultLat = 17.3850;
let defaultLon = 78.4867;
let defaultCity = "Hyderabad, India";

async function fetchWeather() {
  getWeatherData(defaultLat, defaultLon, defaultCity);
}

async function getWeatherData(lat, lon, locationName) {
  try {
    const response = await fetch(`${WEATHER_API_BASE}?latitude=${lat}&longitude=${lon}&current_weather=true&hourly=precipitation_probability&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=10`);
    const data = await response.json();
    
    if (data && data.current_weather) {
      let rainProb = 0;
      if (data.hourly && data.hourly.precipitation_probability) {
         const currentHour = new Date().getHours();
         rainProb = data.hourly.precipitation_probability[currentHour] || 0;
      }
      updateWeatherUI(data.current_weather, locationName, rainProb, data.daily);
    }
  } catch (err) {
    console.error("Failed to fetch weather data:", err);
    document.getElementById("weather-desc").textContent = "Service Unavailable";
  }
}

function updateWeatherUI(current_weather, locationName, rainProb = 0, daily = null) {
  const tempElement = document.getElementById("temperature");
  const iconElement = document.getElementById("weather-icon");
  const descElement = document.getElementById("weather-desc");
  const locElement = document.getElementById("weather-location");
  const rainProbElement = document.getElementById("rain-prob");

  const temp = Math.round(current_weather.temperature);
  const code = current_weather.weathercode;

  tempElement.textContent = temp;
  locElement.textContent = locationName;
  if (rainProbElement) rainProbElement.textContent = `${rainProb}%`;

  let icon = "☀️";
  let desc = "Clear";

  if (code === 0) { icon = "☀️"; desc = "Clear Sky"; }
  else if (code === 1 || code === 2 || code === 3) { icon = "⛅"; desc = "Partly Cloudy"; }
  else if (code === 45 || code === 48) { icon = "🌫️"; desc = "Fog"; }
  else if (code >= 51 && code <= 55) { icon = "🌧️"; desc = "Drizzle"; }
  else if (code >= 61 && code <= 65) { icon = "🌧️"; desc = "Rain"; }
  else if (code >= 71 && code <= 77) { icon = "❄️"; desc = "Snow"; }
  else if (code >= 80 && code <= 82) { icon = "🌦️"; desc = "Showers"; }
  else if (code >= 95 && code <= 99) { icon = "⛈️"; desc = "Thunderstorm"; }

  iconElement.textContent = icon;
  descElement.textContent = desc;

  if (icon === "❄️" || icon === "🌧️") {
     document.documentElement.style.setProperty('--primary', '#3b82f6');
     document.documentElement.style.setProperty('--secondary', '#0ea5e9');
  } else if (icon === "☀️") {
     document.documentElement.style.setProperty('--primary', '#f59e0b');
     document.documentElement.style.setProperty('--accent', '#ef4444');
  }

  if (daily && daily.time) {
     const tableBody = document.getElementById('forecast-table-body');
     if (tableBody) {
        tableBody.innerHTML = '';
        for (let i = 0; i < daily.time.length; i++) {
           const dateStr = daily.time[i];
           const maxT = Math.round(daily.temperature_2m_max[i]);
           const minT = Math.round(daily.temperature_2m_min[i]);
           const rainP = daily.precipitation_probability_max ? daily.precipitation_probability_max[i] : 0;
           const dCode = daily.weathercode[i];

           const dateObj = new Date(dateStr);
           const dayName = i === 0 ? 'Today' : dateObj.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });

           let dIcon = "☀️"; let dDesc = "Clear";
           if (dCode === 0) { dIcon = "☀️"; dDesc = "Clear"; }
           else if (dCode === 1 || dCode === 2 || dCode === 3) { dIcon = "⛅"; dDesc = "Partly Cloudy"; }
           else if (dCode === 45 || dCode === 48) { dIcon = "🌫️"; dDesc = "Fog"; }
           else if (dCode >= 51 && dCode <= 55) { dIcon = "🌧️"; dDesc = "Drizzle"; }
           else if (dCode >= 61 && dCode <= 65) { dIcon = "🌧️"; dDesc = "Rain"; }
           else if (dCode >= 71 && dCode <= 77) { dIcon = "❄️"; dDesc = "Snow"; }
           else if (dCode >= 80 && dCode <= 82) { dIcon = "🌦️"; dDesc = "Showers"; }
           else if (dCode >= 95 && dCode <= 99) { dIcon = "⛈️"; dDesc = "Storm"; }

           const rowStr = `
             <tr>
               <td>${dayName}</td>
               <td><span class="f-icon">${dIcon}</span> ${dDesc}</td>
               <td>${maxT}&deg;C</td>
               <td class="low">${minT}&deg;C</td>
               <td>${rainP}%</td>
             </tr>
           `;
           tableBody.insertAdjacentHTML('beforeend', rowStr);
        }
     }
  }
}

function initWeatherSearch() {
  const searchBtn = document.getElementById('weather-search-btn');
  const searchInput = document.getElementById('weather-search-input');
  
  if (!searchBtn || !searchInput) return;

  const handleSearch = async () => {
    const query = searchInput.value.trim();
    if (!query) return;
    
    try {
      const response = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(query)}&count=1&language=en&format=json`);
      const data = await response.json();
      
      if (data.results && data.results.length > 0) {
         const result = data.results[0];
         let locName = result.name;
         if (result.admin1) locName += `, ${result.admin1}`;
         if (result.country) locName += `, ${result.country}`;
         
         getWeatherData(result.latitude, result.longitude, locName);
         searchInput.value = '';
      } else {
         alert("Location not found. Please try another Zipcode or City.");
      }
    } catch (err) {
      console.error("Geocoding failed", err);
      alert("Search failed. Please try again.");
    }
  };

  searchBtn.addEventListener('click', handleSearch);
  searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handleSearch();
  });
}
