/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.portal.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.client.portal.db.repo.ClientResourceRepository;
import com.axelor.apps.client.portal.db.repo.DiscussionGroupRepository;
import com.axelor.apps.client.portal.db.repo.GeneralAnnouncementRepository;
import com.axelor.apps.client.portal.db.repo.IdeaRepository;
import com.axelor.apps.client.portal.db.repo.PortalIdeaTagRepository;
import com.axelor.apps.client.portal.db.repo.PortalQuotationRepository;
import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.helpdesk.db.repo.TicketRepository;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.ProjectTask;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.apps.project.db.repo.ProjectTaskRepository;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientViewServiceImpl implements ClientViewService {

  protected SaleOrderRepository saleOrderRepo;
  protected StockMoveRepository stockMoveRepo;
  protected ProjectRepository projectRepo;
  protected TicketRepository ticketRepo;
  protected InvoiceRepository invoiceRepo;
  protected ProjectTaskRepository projectTaskRepo;
  protected JpaSecurity security;
  protected AppService appService;
  protected PortalQuotationRepository portalQuotationRepo;
  protected DiscussionGroupRepository discussionGroupRepo;
  protected GeneralAnnouncementRepository announcementRepo;
  protected ClientResourceRepository clientResourceRepo;
  protected IdeaRepository ideaRepo;
  protected PortalIdeaTagRepository ideaTagRepo;

  protected static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  static final String CLIENT_PORTAL_NO_DATE = /*$$(*/ "None" /*)*/;

  @Inject
  public ClientViewServiceImpl(
      SaleOrderRepository saleOrderRepo,
      StockMoveRepository stockMoveRepo,
      ProjectRepository projectRepo,
      TicketRepository ticketRepo,
      InvoiceRepository invoiceRepo,
      ProjectTaskRepository projectTaskRepo,
      JpaSecurity jpaSecurity,
      AppService appService,
      PortalQuotationRepository portalQuotationRepo,
      DiscussionGroupRepository discussionGroupRepo,
      GeneralAnnouncementRepository announcementRepo,
      ClientResourceRepository clientResourceRepo,
      IdeaRepository ideaRepo,
      PortalIdeaTagRepository ideaTagRepo) {
    this.saleOrderRepo = saleOrderRepo;
    this.stockMoveRepo = stockMoveRepo;
    this.projectRepo = projectRepo;
    this.ticketRepo = ticketRepo;
    this.invoiceRepo = invoiceRepo;
    this.projectTaskRepo = projectTaskRepo;
    this.security = jpaSecurity;
    this.appService = appService;
    this.portalQuotationRepo = portalQuotationRepo;
    this.discussionGroupRepo = discussionGroupRepo;
    this.announcementRepo = announcementRepo;
    this.clientResourceRepo = clientResourceRepo;
    this.ideaRepo = ideaRepo;
    this.ideaTagRepo = ideaTagRepo;
  }

  @Override
  public User getClientUser() {
    return Beans.get(UserService.class).getUser();
  }

  @Override
  public Map<String, Object> updateClientViewIndicators() {
    Map<String, Object> map = new HashMap<>();
    User user = getClientUser();
    /* SaleOrder */
    map.put("$ordersInProgress", getOrdersInProgressIndicator(user));
    map.put("$myQuotation", getQuotationsIndicator(user));
    map.put("$lastOrder", getLastOrderIndicator(user));
    /* StockMove */
    map.put("$lastDelivery", getLastDeliveryIndicator(user));
    map.put("$nextDelivery", getNextDeliveryIndicator(user));
    map.put("$plannedDeliveries", getPlannedDeliveriesIndicator(user));
    map.put("$myReversions", getReversionsIndicator(user));
    /* Invoice */
    map.put("$overdueInvoices", getOverdueInvoicesIndicator(user));
    map.put("$awaitingInvoices", getAwaitingInvoicesIndicator(user));
    map.put("$totalRemaining", getTotalRemainingIndicator(user));
    map.put("$myRefund", getRefundIndicator(user));
    /* Helpdesk */
    map.put("$customerTickets", getCustomerTicketsIndicator(user));
    map.put("$companyTickets", getCompanyTicketsIndicator(user));
    map.put("$resolvedTickets", getResolvedTicketsIndicator(user));
    map.put("$lateTickets", getLateTicketsIndicator(user));
    /* Project */
    map.put("$totalProjects", getTotalProjectsIndicator(user));
    map.put("$tasksInCompleted", getTasksInCompletedIndicator(user));
    map.put("$tasksDue", getTasksDueIndicator(user));
    return map;
  }

  /* SaleOrder Indicators */
  protected Integer getOrdersInProgressIndicator(User user) {
    List<Filter> filters = getOrdersInProgressOfUser(user);
    List<SaleOrder> saleOrderList = Filter.and(filters).build(SaleOrder.class).fetch();
    return !saleOrderList.isEmpty() ? saleOrderList.size() : 0;
  }

  protected Integer getQuotationsIndicator(User user) {
    List<Filter> filters = getQuotationsOfUser(user);
    List<SaleOrder> saleOrderList = Filter.and(filters).build(SaleOrder.class).fetch();
    return !saleOrderList.isEmpty() ? saleOrderList.size() : 0;
  }

  protected String getLastOrderIndicator(User user) {
    List<Filter> filters = getLastOrderOfUser(user);
    SaleOrder saleOrder =
        Filter.and(filters).build(SaleOrder.class).order("-confirmationDateTime").fetchOne();
    if (saleOrder == null) {
      return I18n.get(CLIENT_PORTAL_NO_DATE);
    }
    return saleOrder.getConfirmationDateTime() != null
        ? saleOrder.getConfirmationDateTime().format(DATE_FORMATTER)
        : I18n.get(CLIENT_PORTAL_NO_DATE);
  }

  /* StockMove Indicators */
  protected String getLastDeliveryIndicator(User user) {
    List<Filter> filters = getLastDeliveryOfUser(user);
    StockMove stockMove = Filter.and(filters).build(StockMove.class).order("-realDate").fetchOne();
    if (stockMove == null) {
      return I18n.get(CLIENT_PORTAL_NO_DATE);
    }
    return stockMove.getRealDate() != null
        ? stockMove.getRealDate().format(DATE_FORMATTER)
        : I18n.get(CLIENT_PORTAL_NO_DATE);
  }

  protected String getNextDeliveryIndicator(User user) {
    List<Filter> filters = getNextDeliveryOfUser(user);
    StockMove stockMove =
        Filter.and(filters).build(StockMove.class).order("estimatedDate").fetchOne();
    if (stockMove == null) {
      return I18n.get(CLIENT_PORTAL_NO_DATE);
    }
    return stockMove.getEstimatedDate() != null
        ? stockMove.getEstimatedDate().format(DATE_FORMATTER)
        : I18n.get(CLIENT_PORTAL_NO_DATE);
  }

  protected Integer getPlannedDeliveriesIndicator(User user) {
    List<Filter> filters = getPlannedDeliveriesOfUser(user);
    List<StockMove> stockMoveList = Filter.and(filters).build(StockMove.class).fetch();
    return !stockMoveList.isEmpty() ? stockMoveList.size() : 0;
  }

  protected Integer getReversionsIndicator(User user) {
    List<Filter> filters = getReversionsOfUser(user);
    List<StockMove> stockMoveList = Filter.and(filters).build(StockMove.class).fetch();
    return !stockMoveList.isEmpty() ? stockMoveList.size() : 0;
  }

  /* Invoice Indicators */
  protected Integer getOverdueInvoicesIndicator(User user) {
    List<Filter> filters = getOverdueInvoicesOfUser(user);
    List<Invoice> invoiceList = Filter.and(filters).build(Invoice.class).fetch();
    return !invoiceList.isEmpty() ? invoiceList.size() : 0;
  }

  protected Integer getAwaitingInvoicesIndicator(User user) {
    List<Filter> filters = getAwaitingInvoicesOfUser(user);
    List<Invoice> invoiceList = Filter.and(filters).build(Invoice.class).fetch();
    return !invoiceList.isEmpty() ? invoiceList.size() : 0;
  }

  protected String getTotalRemainingIndicator(User user) {
    List<Filter> filters = getTotalRemainingOfUser(user);
    List<Invoice> invoiceList = Filter.and(filters).build(Invoice.class).fetch();
    if (!invoiceList.isEmpty()) {
      BigDecimal total =
          invoiceList.stream()
              .map(Invoice::getAmountRemaining)
              .reduce((x, y) -> x.add(y))
              .orElse(BigDecimal.ZERO);
      return total.toString() + invoiceList.get(0).getCurrency().getSymbol();
    }
    return BigDecimal.ZERO.toString();
  }

  protected Integer getRefundIndicator(User user) {
    List<Filter> filters = getRefundOfUser(user);
    List<Invoice> invoiceList = Filter.and(filters).build(Invoice.class).fetch();
    return !invoiceList.isEmpty() ? invoiceList.size() : 0;
  }

  /* Helpdesk Indicators */
  protected Integer getCustomerTicketsIndicator(User user) {
    List<Filter> filters = getTicketsOfUser(user);
    List<Ticket> ticketList = Filter.and(filters).build(Ticket.class).fetch();
    return !ticketList.isEmpty() ? ticketList.size() : 0;
  }

  protected Integer getCompanyTicketsIndicator(User user) {
    List<Filter> filters = getCompanyTicketsOfUser(user);
    List<Ticket> ticketList = Filter.and(filters).build(Ticket.class).fetch();
    return !ticketList.isEmpty() ? ticketList.size() : 0;
  }

  protected Integer getResolvedTicketsIndicator(User user) {
    List<Filter> filters = getResolvedTicketsOfUser(user);
    List<Ticket> ticketList = Filter.and(filters).build(Ticket.class).fetch();
    return !ticketList.isEmpty() ? ticketList.size() : 0;
  }

  protected Object getLateTicketsIndicator(User user) {
    List<Filter> filters = getLateTicketsOfUser(user);
    List<Ticket> ticketList = Filter.and(filters).build(Ticket.class).fetch();
    return !ticketList.isEmpty() ? ticketList.size() : 0;
  }

  /* Project Indicators */
  protected Integer getTotalProjectsIndicator(User user) {
    List<Filter> filters = getTotalProjectsOfUser(user);
    List<Project> projectList = Filter.and(filters).build(Project.class).fetch();
    return !projectList.isEmpty() ? projectList.size() : 0;
  }

  protected Integer getTasksInCompletedIndicator(User user) {
    List<Filter> filters = getTasksInCompletedOfUser(user);
    List<ProjectTask> projectTaskList = Filter.and(filters).build(ProjectTask.class).fetch();
    return !projectTaskList.isEmpty() ? projectTaskList.size() : 0;
  }

  protected Integer getTasksDueIndicator(User user) {
    List<Filter> filters = getTasksInCompletedOfUser(user);
    List<ProjectTask> projectTaskList = Filter.and(filters).build(ProjectTask.class).fetch();
    return !projectTaskList.isEmpty() ? projectTaskList.size() : 0;
  }

  /* SaleOrder Query */
  @Override
  public List<Filter> getOrdersInProgressOfUser(User user) {

    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, SaleOrder.class);
    Filter filter =
        new JPQLFilter(
            "self.clientPartner.id = "
                + user.getPartner().getId()
                + " AND self.statusSelect = "
                + SaleOrderRepository.STATUS_ORDER_CONFIRMED);

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getQuotationsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, SaleOrder.class);
    Filter filter =
        new JPQLFilter(
            "self.clientPartner.id = "
                + user.getPartner().getId()
                + " AND self.statusSelect IN ("
                + SaleOrderRepository.STATUS_DRAFT_QUOTATION
                + ","
                + SaleOrderRepository.STATUS_FINALIZED_QUOTATION
                + ")");

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }

    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getLastOrderOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, SaleOrder.class);
    Filter filter =
        new JPQLFilter(
            "self.clientPartner.id = "
                + user.getPartner().getId()
                + " AND self.statusSelect = "
                + SaleOrderRepository.STATUS_ORDER_COMPLETED);

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  /* StockMove Query */
  @Override
  public List<Filter> getLastDeliveryOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, StockMove.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.typeSelect = "
                + StockMoveRepository.TYPE_OUTGOING
                + " AND self.statusSelect = "
                + StockMoveRepository.STATUS_REALIZED
                + " AND self.isReversion != true");

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    if (filterFromPermission != null) {
      filter = Filter.and(filter, filterFromPermission);
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getNextDeliveryOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, StockMove.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.typeSelect = "
                + StockMoveRepository.TYPE_OUTGOING
                + " AND self.statusSelect = "
                + StockMoveRepository.STATUS_PLANNED
                + " AND self.isReversion != true");

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  private void addPermissionFilter(List<Filter> filters, Filter filterFromPermission) {
    if (filterFromPermission != null) {
      filters.add(filterFromPermission);
    }
  }

  @Override
  public List<Filter> getPlannedDeliveriesOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, StockMove.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.typeSelect = "
                + StockMoveRepository.TYPE_OUTGOING
                + " AND self.statusSelect = "
                + StockMoveRepository.STATUS_PLANNED
                + " AND self.isReversion != true");
    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getReversionsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, StockMove.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.typeSelect = "
                + StockMoveRepository.TYPE_OUTGOING
                + " AND self.isReversion = true");

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  /* Invoice Query */
  @Override
  public List<Filter> getOverdueInvoicesOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Invoice.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.dueDate < current_date() "
                + " AND self.amountRemaining != 0 AND self.statusSelect != "
                + InvoiceRepository.STATUS_DRAFT
                + " AND self.statusSelect != "
                + InvoiceRepository.STATUS_CANCELED);

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getAwaitingInvoicesOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Invoice.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.amountRemaining != 0 AND self.statusSelect != "
                + InvoiceRepository.STATUS_DRAFT
                + " AND self.statusSelect != "
                + InvoiceRepository.STATUS_CANCELED);

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getTotalRemainingOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Invoice.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.amountRemaining != 0 AND self.statusSelect != "
                + InvoiceRepository.STATUS_DRAFT
                + " AND self.statusSelect != "
                + InvoiceRepository.STATUS_CANCELED);

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);

    return filters;
  }

  @Override
  public List<Filter> getRefundOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Invoice.class);
    Filter filter =
        new JPQLFilter(
            "self.partner.id = "
                + user.getPartner().getId()
                + " AND self.operationTypeSelect = "
                + InvoiceRepository.OPERATION_TYPE_CLIENT_REFUND);

    if (user.getActiveCompany() != null) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);

    return filters;
  }

  /* Helpdesk Query */
  @Override
  public List<Filter> getTicketsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Ticket.class);
    Filter filter =
        new JPQLFilter(
            "self.customer.id = "
                + user.getPartner().getId()
                + " AND self.assignedToUser.id = "
                + user.getId());
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getCompanyTicketsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Ticket.class);
    Filter filter =
        new JPQLFilter(
            "self.customer.id = "
                + user.getPartner().getId()
                + " AND self.assignedToUser.id = "
                + user.getActiveCompany().getId());
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getResolvedTicketsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Ticket.class);
    Filter filter =
        new JPQLFilter(
            "self.customer.id = "
                + user.getPartner().getId()
                + " AND self.assignedToUser.id = "
                + user.getId()
                + " AND self.statusSelect IN ("
                + TicketRepository.STATUS_RESOLVED
                + ", "
                + TicketRepository.STATUS_CLOSED
                + ")");
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getLateTicketsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Ticket.class);
    Filter filter =
        new JPQLFilter(
            "self.customer.id = "
                + user.getPartner().getId()
                + " AND self.assignedToUser.id = "
                + user.getId()
                + " AND ((self.endDateT != null AND self.endDateT > self.deadlineDateT) "
                + " OR (self.endDateT = null and self.deadlineDateT < current_date() ) )");
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  /* Project Query */
  @Override
  public List<Filter> getTotalProjectsOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, Project.class);
    Filter filter =
        new JPQLFilter(
            "self.clientPartner.id = "
                + user.getPartner().getId()
                + " AND self.projectStatus.isCompleted = false");
    if (user.getActiveCompany() != null && appService.isApp("business-project")) {
      filter =
          Filter.and(
              filter, new JPQLFilter(" self.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);

    return filters;
  }

  @Override
  public List<Filter> getTasksInCompletedOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter filterFromPermission = security.getFilter(JpaSecurity.CAN_READ, ProjectTask.class);

    Filter filter =
        new JPQLFilter(
            "self.status.isCompleted = false"
                + " AND self.typeSelect = '"
                + ProjectTaskRepository.TYPE_TASK
                + "' AND self.project.clientPartner.id = "
                + user.getPartner().getId());
    if (user.getActiveCompany() != null && appService.isApp("business-project")) {
      filter =
          Filter.and(
              filter,
              new JPQLFilter(" self.project.company.id = " + user.getActiveCompany().getId()));
    }
    filters.add(filter);
    addPermissionFilter(filters, filterFromPermission);
    return filters;
  }

  @Override
  public List<Filter> getTasksDueOfUser(User user) {
    List<Filter> filters = new ArrayList<>();
    Filter dateFilter = new JPQLFilter("self.taskEndDate  < current_date()");
    filters.add(Filter.and(getTasksInCompletedOfUser(user).get(0), dateFilter));
    return filters;
  }

  @Override
  public Long getOpenQuotation() {
    return portalQuotationRepo
        .all()
        .filter(
            "(self.saleOrder.clientPartner = :clientPartner OR self.saleOrder.contactPartner = :clientPartner) AND "
                + "self IN (SELECT MAX(id) FROM PortalQuotation portalQuotation WHERE self.saleOrder.statusSelect < :status GROUP BY portalQuotation.saleOrder)")
        .bind("status", SaleOrderRepository.STATUS_ORDER_CONFIRMED)
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getQuotationSaleOrder() {
    return portalQuotationRepo
        .all()
        .filter(
            "(self.saleOrder.clientPartner = :clientPartner OR self.saleOrder.contactPartner = :clientPartner) AND "
                + "self IN (SELECT MAX(id) FROM PortalQuotation portalQuotation WHERE self.saleOrder.statusSelect = :status GROUP BY portalQuotation.saleOrder)")
        .bind("status", SaleOrderRepository.STATUS_ORDER_CONFIRMED)
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getQuotationHistory() {
    return portalQuotationRepo
        .all()
        .filter(
            "(self.saleOrder.clientPartner = :clientPartner OR self.saleOrder.contactPartner = :clientPartner) AND "
                + "((self.endOfValidity < :today AND self.saleOrder.electronicSignature IS NULL) OR (self.saleOrder.statusSelect >= :status))")
        .bind("today", Beans.get(AppBaseService.class).getTodayDateTime().toLocalDate())
        .bind("status", SaleOrderRepository.STATUS_ORDER_COMPLETED)
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getAllQuotation() {
    return portalQuotationRepo
        .all()
        .filter(
            "(self.saleOrder.clientPartner = :clientPartner OR self.saleOrder.contactPartner = :clientPartner)")
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getToPayInvoice() {
    return invoiceRepo
        .all()
        .filter(
            "(self.partner = :clientPartner OR self.contactPartner = :clientPartner) AND "
                + "(self.operationTypeSelect = :operationTypeSelect AND self.amountRemaining > 0)")
        .bind("operationTypeSelect", InvoiceRepository.OPERATION_TYPE_CLIENT_SALE)
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getOldInvoice() {
    return invoiceRepo
        .all()
        .filter(
            "(self.partner = :clientPartner OR self.contactPartner = :clientPartner) AND "
                + "(self.operationTypeSelect = :operationTypeSelect AND self.amountRemaining = 0)")
        .bind("operationTypeSelect", InvoiceRepository.OPERATION_TYPE_CLIENT_SALE)
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getRefundInvoice() {
    return invoiceRepo
        .all()
        .filter(
            "(self.partner = :clientPartner OR self.contactPartner = :clientPartner) AND "
                + "(self.operationTypeSelect = :operationTypeSelect)")
        .bind("operationTypeSelect", InvoiceRepository.OPERATION_TYPE_CLIENT_REFUND)
        .bind("clientPartner", getClientUser().getPartner())
        .count();
  }

  @Override
  public Long getMyTicket() {

    User user = getClientUser();
    Partner partner = user.getPartner();
    return projectTaskRepo
        .all()
        .filter(
            "self.assignment = 1 AND self.project.clientPartner = :partner AND self.status.isCompleted != true AND self.typeSelect = :typeSelect")
        .bind(
            "partner",
            partner != null ? partner.getIsContact() ? partner.getMainPartner() : partner : null)
        .bind("typeSelect", ProjectTaskRepository.TYPE_TICKET)
        .count();
  }

  @Override
  public Long getProviderTicket() {

    User user = getClientUser();
    Partner partner = user.getPartner();
    return projectTaskRepo
        .all()
        .filter(
            "self.assignment = 2 AND self.project.clientPartner = :partner AND self.status.isCompleted != true AND self.typeSelect = :typeSelect")
        .bind(
            "partner",
            partner != null ? partner.getIsContact() ? partner.getMainPartner() : partner : null)
        .bind("typeSelect", ProjectTaskRepository.TYPE_TICKET)
        .count();
  }

  @Override
  public Long getOpenTicket() {

    User user = getClientUser();
    Partner partner = user.getPartner();
    return projectTaskRepo
        .all()
        .filter(
            "self.project.clientPartner = :partner AND self.status.isCompleted != true AND self.typeSelect = :typeSelect")
        .bind(
            "partner",
            partner != null ? partner.getIsContact() ? partner.getMainPartner() : partner : null)
        .bind("typeSelect", ProjectTaskRepository.TYPE_TICKET)
        .count();
  }

  @Override
  public Long getCloseTicket() {

    User user = getClientUser();
    Partner partner = user.getPartner();
    return projectTaskRepo
        .all()
        .filter(
            "self.project.clientPartner = :partner AND self.status.isCompleted = true AND self.typeSelect = :typeSelect")
        .bind(
            "partner",
            partner != null ? partner.getIsContact() ? partner.getMainPartner() : partner : null)
        .bind("typeSelect", ProjectTaskRepository.TYPE_TICKET)
        .count();
  }

  @Override
  public Long getDiscussionGroup() {

    Partner partner = getClientUser().getPartner();
    return discussionGroupRepo
        .all()
        .filter(":partnerCategory MEMBER OF self.partnerCategorySet")
        .bind("partnerCategory", partner != null ? partner.getPartnerCategory() : null)
        .count();
  }

  @Override
  public Long getAnnouncement() {

    Partner partner = getClientUser().getPartner();
    return announcementRepo
        .all()
        .filter(":partnerCategory MEMBER OF self.partnerCategorySet")
        .bind("partnerCategory", partner != null ? partner.getPartnerCategory() : null)
        .count();
  }

  @Override
  public Long getResouces() {

    Partner partner = getClientUser().getPartner();
    return clientResourceRepo
        .all()
        .filter(
            ":partnerCategory MEMBER OF self.partnerCategorySet OR size(self.partnerCategorySet) = 0")
        .bind("partnerCategory", partner != null ? partner.getPartnerCategory() : null)
        .count();
  }

  @Override
  public Long getIdea() {
    return ideaRepo.all().filter("self.close = false").count();
  }

  @Override
  public Long getIdeaHistory() {
    return ideaRepo.all().filter("self.close = true").count();
  }

  @Override
  public Long getIdeaTag() {
    return ideaTagRepo.all().count();
  }
}
