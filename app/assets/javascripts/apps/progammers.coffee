#
# Backbone main.js
#

# --- Router ---

AppRouter = Backbone.Router.extend
  routes:
    "": "showProgrammers"
    "programmers": "showProgrammers"
    "companies": "showCompanies"
    "skills": "showSkills"

  showProgrammers: ->
    new ProgrammersView().renew()
  showCompanies: ->
    new CompaniesView().renew()
  showSkills: ->
    new SkillsView().renew()

# --- Programmers ---

Programmer = Backbone.Model.extend
  initialize: ->
    @on('sync', ->
      new ProgrammersView().renew())
  urlRoot: '/programmers'
  validate: (attrs, options) ->
    'name is required' unless attrs.name

  addSkill: (skillId) ->
    route = jsRoutes.controllers.Programmers.addSkill(@id, skillId)
    $.ajax
      url: route.url, type: route.method,
      success: (response) =>
        new ProgrammersView().renew()
      error: (response) =>
        console.log "POST /programmers/#{@id}/skills/#{skillId} failure (status: #{response.statusText})"

  deleteSkill: (skillId) ->
    route = jsRoutes.controllers.Programmers.deleteSkill(@id, skillId)
    $.ajax
      url: route.url, type: route.method,
      success: (response) =>
        new ProgrammersView().renew()
      error: (response) =>
        console.log "DELETE /programmers/#{@id}/skills/#{skillId} failure (status: #{response.statusText})"

  changeCompany: (companyId) ->
    if companyId then route = jsRoutes.controllers.Programmers.joinCompany(@id, companyId)
    else              route = jsRoutes.controllers.Programmers.leaveCompany(@id)
    $.ajax
      url: route.url, type: route.method,
      success: (response) =>
        $("#changeCompanyHolder#{@id}").append($('<i class="icon-ok"></i>'))
      error: (response) =>
        console.log "/programmers/#{@id}/company failure (status: #{response.statusText})"

Programmers = Backbone.Collection.extend
  model: Programmer
  url: '/programmers'

ProgrammerView = Backbone.View.extend
  render: (param) ->
    $('#main').html(@$el.html(_.template($('#main_programmer').html(), param)))

ProgrammersView = Backbone.View.extend
  events:
    "submit  .addProgrammer": "addProgrammer"
    "blur    .addSkill": "addSkill"
    "click   .deleteSkill": "deleteSkill"
    "blur    .changeCompany": "changeCompany"
    "click   .deleteProgrammer": "deleteProgrammer"

  addProgrammer: (event) ->
    event.preventDefault()
    model = new Programmer(name: $('#newName').val(), companyId: $('#newCompanyId').val())
    if model.isValid() then model.save()
    else window.alert model.validationError

  addSkill: (event) ->
    event.preventDefault()
    id = $(event.currentTarget).data('id')
    skillId = $(event.currentTarget).val()
    programmers.get(id).addSkill(skillId)

  deleteSkill: (event) ->
    event.preventDefault()
    id = $(event.currentTarget).data('programmer-name')
    skillId = $(event.currentTarget).data('skill-name')
    programmers.get(id).deleteSkill(skillId)

  changeCompany: (event) ->
    event.preventDefault()
    id = $(event.currentTarget).data('id')
    companyId = $(event.currentTarget).val()
    programmers.get(id).changeCompany(companyId)

  deleteProgrammer: (event) ->
    if window.confirm("Are you sure?")
      event.preventDefault()
      id = $(event.currentTarget).data('id')
      programmers.get(id).destroy()

  render: (param) ->
    @$el.html(_.template($('#main_programmers').html(), param))

  renew: ->
    $.when(
      programmers.fetch(), companies.fetch(), skills.fetch()
    ).done(->
      $('#main').html(new ProgrammersView().render(
        programmers: programmers
        companies: companies
        skills: skills
      ))
    )

# --- Companies ---

Company = Backbone.Model.extend
  initialize: ->
    @on('sync', ->
      new CompaniesView().renew())
  urlRoot: '/companies'
  validate: (attrs, options) ->
    'name is required' unless attrs.name

Companies = Backbone.Collection.extend
  model: Company
  url: '/companies'

CompaniesView = Backbone.View.extend
  events:
    "submit  .addCompany": "addCompany"
    "click   .deleteCompany": "deleteCompany"

  addCompany: (event) ->
    event.preventDefault()
    model = new Company(name: $('#newName').val())
    if model.isValid() then model.save()
    else  window.alert(model.validationError)

  deleteCompany: (event) ->
    if window.confirm("Are you sure?")
      event.preventDefault()
      id = $(event.currentTarget).data('id')
      companies.get(id).destroy()

  render: (param) ->
    $('#main').html(@$el.html(_.template($('#main_companies').html(), param)))

  renew: ->
    companies.fetch
      success: (companies) ->
        new CompaniesView().render({companies: companies})
      error: (response) ->
        console.log "GET /companies failure (status: #{response.statusText})"

# --- Skills ---

Skill = Backbone.Model.extend
  initialize: ->
    @on('sync', ->
      new SkillsView().renew())
  urlRoot: '/skills'
  validate: (attrs, options) ->
    'name is required' unless attrs.name

Skills = Backbone.Collection.extend
  model: Skill
  url: '/skills'

SkillsView = Backbone.View.extend
  events:
    "submit  .addSkill": "addSkill"
    "click   .deleteSkill": "deleteSkill"

  addSkill: (event) ->
    event.preventDefault()
    model = new Skill(name: $('#newName').val())
    if model.isValid() then model.save()
    else window.alert model.validationError

  deleteSkill: (event) ->
    if window.confirm("Are you sure?")
      event.preventDefault()
      id = $(event.currentTarget).data('id')
      skills.get(id).destroy()

  render: (param) ->
    $('#main').html(@$el.html(_.template($('#main_skills').html(), param)))

  renew: ->
    skills.fetch
      success: (skills) ->
        new SkillsView().render({skills: skills})
      error: (response) ->
        console.log "GET /skills failure (status: #{response.statusText})"


# --- Initialize ---

programmers = new Programmers()
companies = new Companies()
skills = new Skills()

$ ->
  appRouter = new AppRouter()
  Backbone.history.start
    pushHistory: true

