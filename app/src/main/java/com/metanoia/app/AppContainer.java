package com.metanoia.app;

import android.content.Context;

import com.metanoia.app.data.backup.BackupManager;
import com.metanoia.app.data.backup.BackupScheduler;
import com.metanoia.app.data.backup.RestoreManager;
import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.data.repository.AppointmentRepositoryImpl;
import com.metanoia.app.data.repository.AuditLogRepositoryImpl;
import com.metanoia.app.data.repository.CashRepositoryImpl;
import com.metanoia.app.data.repository.ClientRepositoryImpl;
import com.metanoia.app.data.repository.ColorFormulaRepositoryImpl;
import com.metanoia.app.data.repository.ProfessionalRepositoryImpl;
import com.metanoia.app.data.repository.ReminderRepositoryImpl;
import com.metanoia.app.data.repository.SalonServiceRepositoryImpl;
import com.metanoia.app.data.repository.ScheduleBlockRepositoryImpl;
import com.metanoia.app.data.repository.SettingsRepositoryImpl;
import com.metanoia.app.data.repository.StockRepositoryImpl;
import com.metanoia.app.data.repository.WaitlistRepositoryImpl;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ColorFormulaRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.ReminderRepository;
import com.metanoia.domain.repository.SalonServiceRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.repository.WaitlistRepository;
import com.metanoia.domain.usecase.CancelRecurringSeriesUseCase;
import com.metanoia.domain.usecase.AssignWaitlistEntryUseCase;
import com.metanoia.domain.usecase.CancelWaitlistEntryUseCase;
import com.metanoia.domain.usecase.ChangeAppointmentStatusUseCase;
import com.metanoia.domain.usecase.ChargeAppointmentUseCase;
import com.metanoia.domain.usecase.CloseCashDayUseCase;
import com.metanoia.domain.usecase.CompleteAndChargeUseCase;
import com.metanoia.domain.usecase.RecordCashMovementUseCase;
import com.metanoia.domain.usecase.CreateAppointmentUseCase;
import com.metanoia.domain.usecase.CreateScheduleBlockUseCase;
import com.metanoia.domain.usecase.DeactivateScheduleBlockUseCase;
import com.metanoia.domain.usecase.DeleteColorFormulaUseCase;
import com.metanoia.domain.usecase.DuplicateColorFormulaUseCase;
import com.metanoia.domain.usecase.ExpireWaitlistEntriesUseCase;
import com.metanoia.domain.usecase.FindWaitlistCandidatesUseCase;
import com.metanoia.domain.usecase.GenerateRecurringAppointmentsUseCase;
import com.metanoia.domain.usecase.GetSettingsUseCase;
import com.metanoia.domain.usecase.ManageProfessionalDayOffUseCase;
import com.metanoia.domain.usecase.OfferWaitlistEntryUseCase;
import com.metanoia.domain.usecase.RecordReminderSentUseCase;
import com.metanoia.domain.usecase.RecordStockMovementUseCase;
import com.metanoia.domain.usecase.RescheduleAppointmentUseCase;
import com.metanoia.domain.usecase.ReverseMovementUseCase;
import com.metanoia.domain.usecase.SaveClientUseCase;
import com.metanoia.domain.usecase.SaveColorFormulaUseCase;
import com.metanoia.domain.usecase.SaveProductUseCase;
import com.metanoia.domain.usecase.SaveProfessionalUseCase;
import com.metanoia.domain.usecase.SaveSalonServiceUseCase;
import com.metanoia.domain.usecase.SaveWaitlistEntryUseCase;
import com.metanoia.domain.usecase.SellProductUseCase;
import com.metanoia.domain.usecase.SetClientActiveUseCase;
import com.metanoia.domain.usecase.SetProductActiveUseCase;
import com.metanoia.domain.usecase.SetProfessionalActiveUseCase;
import com.metanoia.domain.usecase.SetSalonServiceActiveUseCase;
import com.metanoia.domain.usecase.UpdateAppointmentUseCase;
import com.metanoia.domain.usecase.UpdateSettingsUseCase;

/**
 * Contenedor manual de dependencias de la aplicación.
 */
public final class AppContainer {

    private final AppExecutors executors;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DevicePrefs devicePrefs;
    private final AppDatabase database;
    private final AuditLogRepository auditLogRepository;
    private final SettingsRepository settingsRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final CashRepository cashRepository;
    private final ReminderRepository reminderRepository;
    private final ColorFormulaRepository colorFormulaRepository;
    private final WaitlistRepository waitlistRepository;
    private final StockRepository stockRepository;
    private final GetSettingsUseCase getSettingsUseCase;
    private final UpdateSettingsUseCase updateSettingsUseCase;
    private final SaveSalonServiceUseCase saveSalonServiceUseCase;
    private final SetSalonServiceActiveUseCase setSalonServiceActiveUseCase;
    private final SaveProfessionalUseCase saveProfessionalUseCase;
    private final SetProfessionalActiveUseCase setProfessionalActiveUseCase;
    private final ManageProfessionalDayOffUseCase manageProfessionalDayOffUseCase;
    private final SaveClientUseCase saveClientUseCase;
    private final SetClientActiveUseCase setClientActiveUseCase;
    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final UpdateAppointmentUseCase updateAppointmentUseCase;
    private final ChangeAppointmentStatusUseCase changeAppointmentStatusUseCase;
    private final RescheduleAppointmentUseCase rescheduleAppointmentUseCase;
    private final CreateScheduleBlockUseCase createScheduleBlockUseCase;
    private final DeactivateScheduleBlockUseCase deactivateScheduleBlockUseCase;
    private final GenerateRecurringAppointmentsUseCase generateRecurringAppointmentsUseCase;
    private final CancelRecurringSeriesUseCase cancelRecurringSeriesUseCase;
    private final ReverseMovementUseCase reverseMovementUseCase;
    private final ChargeAppointmentUseCase chargeAppointmentUseCase;
    private final CompleteAndChargeUseCase completeAndChargeUseCase;
    private final RecordCashMovementUseCase recordCashMovementUseCase;
    private final CloseCashDayUseCase closeCashDayUseCase;
    private final RecordReminderSentUseCase recordReminderSentUseCase;
    private final SaveColorFormulaUseCase saveColorFormulaUseCase;
    private final DuplicateColorFormulaUseCase duplicateColorFormulaUseCase;
    private final DeleteColorFormulaUseCase deleteColorFormulaUseCase;
    private final SaveWaitlistEntryUseCase saveWaitlistEntryUseCase;
    private final CancelWaitlistEntryUseCase cancelWaitlistEntryUseCase;
    private final ExpireWaitlistEntriesUseCase expireWaitlistEntriesUseCase;
    private final FindWaitlistCandidatesUseCase findWaitlistCandidatesUseCase;
    private final OfferWaitlistEntryUseCase offerWaitlistEntryUseCase;
    private final AssignWaitlistEntryUseCase assignWaitlistEntryUseCase;
    private final SaveProductUseCase saveProductUseCase;
    private final SetProductActiveUseCase setProductActiveUseCase;
    private final RecordStockMovementUseCase recordStockMovementUseCase;
    private final SellProductUseCase sellProductUseCase;
    private final BackupManager backupManager;
    private final RestoreManager restoreManager;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        this.executors = new AppExecutors();
        this.clock = Clock.systemArgentina();
        this.idGenerator = IdGenerator.uuid();
        this.devicePrefs = new DevicePrefs(appContext);
        this.database = AppDatabase.create(appContext);
        this.auditLogRepository = new AuditLogRepositoryImpl(database.auditLogDao());
        this.settingsRepository = new SettingsRepositoryImpl(database.settingsDao(), clock);
        this.salonServiceRepository = new SalonServiceRepositoryImpl(database.salonServiceDao());
        this.professionalRepository = new ProfessionalRepositoryImpl(
                database.professionalDao(), database.appointmentDao());
        this.clientRepository = new ClientRepositoryImpl(database.clientDao());
        this.appointmentRepository = new AppointmentRepositoryImpl(
                database.appointmentDao(), database.recurrenceRuleDao());
        this.scheduleBlockRepository = new ScheduleBlockRepositoryImpl(database.scheduleBlockDao());
        this.cashRepository = new CashRepositoryImpl(database);
        this.reminderRepository = new ReminderRepositoryImpl(database.reminderDao());
        this.colorFormulaRepository = new ColorFormulaRepositoryImpl(database.colorFormulaDao());
        this.waitlistRepository = new WaitlistRepositoryImpl(database.waitlistDao());
        this.stockRepository = new StockRepositoryImpl(database);
        this.getSettingsUseCase = new GetSettingsUseCase(settingsRepository);
        this.updateSettingsUseCase = new UpdateSettingsUseCase(
                settingsRepository, auditLogRepository, clock, idGenerator);
        this.saveSalonServiceUseCase = new SaveSalonServiceUseCase(
                salonServiceRepository, settingsRepository, clock, idGenerator);
        this.setSalonServiceActiveUseCase = new SetSalonServiceActiveUseCase(
                salonServiceRepository, clock);
        this.saveProfessionalUseCase = new SaveProfessionalUseCase(
                professionalRepository, salonServiceRepository, settingsRepository,
                clock, idGenerator);
        this.setProfessionalActiveUseCase = new SetProfessionalActiveUseCase(
                professionalRepository, clock);
        this.manageProfessionalDayOffUseCase = new ManageProfessionalDayOffUseCase(
                professionalRepository, clock);
        this.saveClientUseCase = new SaveClientUseCase(
                clientRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.setClientActiveUseCase = new SetClientActiveUseCase(
                clientRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.createAppointmentUseCase = new CreateAppointmentUseCase(
                appointmentRepository, clientRepository, professionalRepository,
                salonServiceRepository, scheduleBlockRepository, settingsRepository,
                auditLogRepository, colorFormulaRepository, clock, idGenerator);
        this.updateAppointmentUseCase = new UpdateAppointmentUseCase(
                appointmentRepository, professionalRepository, salonServiceRepository,
                scheduleBlockRepository, settingsRepository, auditLogRepository,
                clock, idGenerator);
        this.changeAppointmentStatusUseCase = new ChangeAppointmentStatusUseCase(
                appointmentRepository, clientRepository, professionalRepository,
                settingsRepository, auditLogRepository, clock, idGenerator);
        this.rescheduleAppointmentUseCase = new RescheduleAppointmentUseCase(
                appointmentRepository, professionalRepository, scheduleBlockRepository,
                settingsRepository, auditLogRepository, clock, idGenerator);
        this.createScheduleBlockUseCase = new CreateScheduleBlockUseCase(
                scheduleBlockRepository, appointmentRepository, professionalRepository,
                settingsRepository, auditLogRepository, clock, idGenerator);
        this.deactivateScheduleBlockUseCase = new DeactivateScheduleBlockUseCase(
                scheduleBlockRepository, settingsRepository, auditLogRepository,
                clock, idGenerator);
        this.generateRecurringAppointmentsUseCase = new GenerateRecurringAppointmentsUseCase(
                appointmentRepository, clientRepository, professionalRepository,
                salonServiceRepository, scheduleBlockRepository, settingsRepository,
                auditLogRepository, colorFormulaRepository, clock, idGenerator);
        this.cancelRecurringSeriesUseCase = new CancelRecurringSeriesUseCase(
                appointmentRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.reverseMovementUseCase = new ReverseMovementUseCase(
                cashRepository, appointmentRepository, stockRepository, settingsRepository,
                auditLogRepository, clock, idGenerator);
        this.chargeAppointmentUseCase = new ChargeAppointmentUseCase(
                appointmentRepository, cashRepository, settingsRepository,
                auditLogRepository, clock, idGenerator);
        this.completeAndChargeUseCase = new CompleteAndChargeUseCase(
                appointmentRepository, professionalRepository, cashRepository,
                settingsRepository, auditLogRepository, clock, idGenerator);
        this.recordCashMovementUseCase = new RecordCashMovementUseCase(
                cashRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.closeCashDayUseCase = new CloseCashDayUseCase(
                cashRepository, settingsRepository, auditLogRepository,
                clock, idGenerator,
                closedDayEpoch -> BackupScheduler.enqueueCashClose(appContext));
        this.recordReminderSentUseCase = new RecordReminderSentUseCase(
                reminderRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.saveColorFormulaUseCase = new SaveColorFormulaUseCase(
                colorFormulaRepository, clientRepository, professionalRepository,
                appointmentRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.duplicateColorFormulaUseCase = new DuplicateColorFormulaUseCase(
                colorFormulaRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.deleteColorFormulaUseCase = new DeleteColorFormulaUseCase(
                colorFormulaRepository, appointmentRepository, settingsRepository,
                auditLogRepository, clock, idGenerator);
        this.saveWaitlistEntryUseCase = new SaveWaitlistEntryUseCase(
                waitlistRepository, clientRepository, salonServiceRepository,
                professionalRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.cancelWaitlistEntryUseCase = new CancelWaitlistEntryUseCase(
                waitlistRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.expireWaitlistEntriesUseCase = new ExpireWaitlistEntriesUseCase(
                waitlistRepository, clock);
        this.findWaitlistCandidatesUseCase = new FindWaitlistCandidatesUseCase(
                waitlistRepository, professionalRepository, clientRepository,
                salonServiceRepository, settingsRepository, clock);
        this.offerWaitlistEntryUseCase = new OfferWaitlistEntryUseCase(
                waitlistRepository, clientRepository, professionalRepository,
                salonServiceRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.assignWaitlistEntryUseCase = new AssignWaitlistEntryUseCase(
                waitlistRepository, createAppointmentUseCase, settingsRepository,
                auditLogRepository, clock, idGenerator);
        this.saveProductUseCase = new SaveProductUseCase(
                stockRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.setProductActiveUseCase = new SetProductActiveUseCase(
                stockRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.recordStockMovementUseCase = new RecordStockMovementUseCase(
                stockRepository, settingsRepository, auditLogRepository, clock, idGenerator);
        this.sellProductUseCase = new SellProductUseCase(
                stockRepository, cashRepository, settingsRepository,
                auditLogRepository, clock, idGenerator);
        this.backupManager = new BackupManager(
                appContext, database, devicePrefs, auditLogRepository,
                settingsRepository, clock, idGenerator);
        this.restoreManager = new RestoreManager(
                appContext, database, devicePrefs, backupManager);
    }

    public AppExecutors executors() {
        return executors;
    }

    public Clock clock() {
        return clock;
    }

    public IdGenerator idGenerator() {
        return idGenerator;
    }

    public DevicePrefs devicePrefs() {
        return devicePrefs;
    }

    public AppDatabase database() {
        return database;
    }

    public AuditLogRepository auditLogRepository() {
        return auditLogRepository;
    }

    public SettingsRepository settingsRepository() {
        return settingsRepository;
    }

    public SalonServiceRepository salonServiceRepository() {
        return salonServiceRepository;
    }

    public ProfessionalRepository professionalRepository() {
        return professionalRepository;
    }

    public ClientRepository clientRepository() {
        return clientRepository;
    }

    public AppointmentRepository appointmentRepository() {
        return appointmentRepository;
    }

    public ScheduleBlockRepository scheduleBlockRepository() {
        return scheduleBlockRepository;
    }

    public GetSettingsUseCase getSettingsUseCase() {
        return getSettingsUseCase;
    }

    public UpdateSettingsUseCase updateSettingsUseCase() {
        return updateSettingsUseCase;
    }

    public SaveSalonServiceUseCase saveSalonServiceUseCase() {
        return saveSalonServiceUseCase;
    }

    public SetSalonServiceActiveUseCase setSalonServiceActiveUseCase() {
        return setSalonServiceActiveUseCase;
    }

    public SaveProfessionalUseCase saveProfessionalUseCase() {
        return saveProfessionalUseCase;
    }

    public SetProfessionalActiveUseCase setProfessionalActiveUseCase() {
        return setProfessionalActiveUseCase;
    }

    public ManageProfessionalDayOffUseCase manageProfessionalDayOffUseCase() {
        return manageProfessionalDayOffUseCase;
    }

    public SaveClientUseCase saveClientUseCase() {
        return saveClientUseCase;
    }

    public SetClientActiveUseCase setClientActiveUseCase() {
        return setClientActiveUseCase;
    }

    public CreateAppointmentUseCase createAppointmentUseCase() {
        return createAppointmentUseCase;
    }

    public UpdateAppointmentUseCase updateAppointmentUseCase() {
        return updateAppointmentUseCase;
    }

    public ChangeAppointmentStatusUseCase changeAppointmentStatusUseCase() {
        return changeAppointmentStatusUseCase;
    }

    public RescheduleAppointmentUseCase rescheduleAppointmentUseCase() {
        return rescheduleAppointmentUseCase;
    }

    public CreateScheduleBlockUseCase createScheduleBlockUseCase() {
        return createScheduleBlockUseCase;
    }

    public DeactivateScheduleBlockUseCase deactivateScheduleBlockUseCase() {
        return deactivateScheduleBlockUseCase;
    }

    public GenerateRecurringAppointmentsUseCase generateRecurringAppointmentsUseCase() {
        return generateRecurringAppointmentsUseCase;
    }

    public CancelRecurringSeriesUseCase cancelRecurringSeriesUseCase() {
        return cancelRecurringSeriesUseCase;
    }

    public BackupManager backupManager() {
        return backupManager;
    }

    public RestoreManager restoreManager() {
        return restoreManager;
    }

    public CashRepository cashRepository() {
        return cashRepository;
    }

    public ReverseMovementUseCase reverseMovementUseCase() {
        return reverseMovementUseCase;
    }

    public ChargeAppointmentUseCase chargeAppointmentUseCase() {
        return chargeAppointmentUseCase;
    }

    public CompleteAndChargeUseCase completeAndChargeUseCase() {
        return completeAndChargeUseCase;
    }

    public RecordCashMovementUseCase recordCashMovementUseCase() {
        return recordCashMovementUseCase;
    }

    public CloseCashDayUseCase closeCashDayUseCase() {
        return closeCashDayUseCase;
    }

    public ReminderRepository reminderRepository() {
        return reminderRepository;
    }

    public RecordReminderSentUseCase recordReminderSentUseCase() {
        return recordReminderSentUseCase;
    }

    public ColorFormulaRepository colorFormulaRepository() {
        return colorFormulaRepository;
    }

    public SaveColorFormulaUseCase saveColorFormulaUseCase() {
        return saveColorFormulaUseCase;
    }

    public DuplicateColorFormulaUseCase duplicateColorFormulaUseCase() {
        return duplicateColorFormulaUseCase;
    }

    public DeleteColorFormulaUseCase deleteColorFormulaUseCase() {
        return deleteColorFormulaUseCase;
    }

    public WaitlistRepository waitlistRepository() {
        return waitlistRepository;
    }

    public SaveWaitlistEntryUseCase saveWaitlistEntryUseCase() {
        return saveWaitlistEntryUseCase;
    }

    public CancelWaitlistEntryUseCase cancelWaitlistEntryUseCase() {
        return cancelWaitlistEntryUseCase;
    }

    public ExpireWaitlistEntriesUseCase expireWaitlistEntriesUseCase() {
        return expireWaitlistEntriesUseCase;
    }

    public FindWaitlistCandidatesUseCase findWaitlistCandidatesUseCase() {
        return findWaitlistCandidatesUseCase;
    }

    public OfferWaitlistEntryUseCase offerWaitlistEntryUseCase() {
        return offerWaitlistEntryUseCase;
    }

    public AssignWaitlistEntryUseCase assignWaitlistEntryUseCase() {
        return assignWaitlistEntryUseCase;
    }

    public StockRepository stockRepository() {
        return stockRepository;
    }

    public SaveProductUseCase saveProductUseCase() {
        return saveProductUseCase;
    }

    public SetProductActiveUseCase setProductActiveUseCase() {
        return setProductActiveUseCase;
    }

    public RecordStockMovementUseCase recordStockMovementUseCase() {
        return recordStockMovementUseCase;
    }

    public SellProductUseCase sellProductUseCase() {
        return sellProductUseCase;
    }
}
