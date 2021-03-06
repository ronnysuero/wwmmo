
import os
import webapp2 as webapp
from google.appengine.api import users
from google.appengine.api import images
from google.appengine.api import memcache
from google.appengine.ext import blobstore
import datetime
import json

import handlers
import model.blog
import model.doc
import ctrl.blog



class AdminPage(handlers.BaseHandler):
  def dispatch(self):
    '''Checks that the user is logged in and such before we process the request.'''
    self.user = users.get_current_user()
    if not self.user:
      # not logged in, so redirect to the login page
      self.redirect(users.create_login_url(self.request.uri))
      return

    # TODO: better handling of authorization...
    if self.user.email() != 'dean@codeka.com.au':
      # not authorized to view the backend, redirect to the home page instead
      self.redirect('/')
      return

    super(AdminPage, self).dispatch()


class AdminDashboardPage(AdminPage):
  def get(self):
    self.render('admin/dashboard.html', {})


class AdminPostListPage(AdminPage):
  def get(self):
    pageNo = 0
    if self.request.get('page'):
      pageNo = int(self.request.get('page'))

    posts = ctrl.blog.getPosts(pageNo, 100, True)

    data = {'posts': posts,
            'pageNo': pageNo}
    self.render('admin/posts_list.html', data)


class AdminPostsPage(AdminPage):
  def get(self, postID = None):
    data = {}
    if postID == 'new':
      self.render('admin/posts_new.html', data)
    else:
      post = ctrl.blog.getPost(int(postID))
      data['post'] = post
      self.render('admin/posts_edit.html', data)

  def post(self, postID):
    if postID == 'new':
      post = model.blog.Post()
    else:
      post = ctrl.blog.getPost(int(postID))
    post.html = self.request.POST.get('post-content')
    post.title = self.request.POST.get('post-title')

    if self.request.POST.get('post-date'):
      post.posted = datetime.datetime.strptime(self.request.POST.get('post-date'), '%y-%m-%d %H:%M')

    post.isPublished = bool(self.request.POST.get('post-ispublished'))

    post.tags = []
    for tag in self.request.POST.get('post-tags').split(','):
      if tag.strip() == '':
        continue
      post.tags.append(tag.strip())

    post.blobs = []
    if self.request.POST.get('post-blobs'):
      for blobKey in json.loads(self.request.POST.get('post-blobs')):
        post.blobs.append(blobKey)

    ctrl.blog.savePost(post)

    if self.request.POST.get('action') == 'Save & View':
      self.redirect('/blog/%04d/%02d/%s' % (post.posted.year, post.posted.month, post.slug))
    else:
      self.redirect('/admin/posts/%d' % (post.key().id()))


class AdminPostDeletePage(AdminPage):
  def get(self, postID):
    data = {'post': ctrl.blog.getPost(int(postID))}
    self.render('admin/post_delete.html', data)

  def post(self, postID):
    ctrl.blog.deletePost(int(postID))
    self.redirect('/admin/posts')


class AdminBlobsPage(AdminPage):
  def get(self):
    query = blobstore.BlobInfo.all().order("-creation")
    if self.request.get("cursor"):
      query.with_cursor(self.request.get("cursor"))
    blobs = []
    for blob in query:
      blobs.append(blob)
      if len(blobs) > 20:
        break
    data = {"blobs": blobs}

    cursor = query.cursor()
    for blob in query:
      # only add the cursor if there's at least one more...
      data["cursor"] = cursor
      break

    self.render('admin/blobs/index.html', data)


class AdminBlobsNewPage(AdminPage):
  def get(self):
    self.render('admin/blobs/new.html', {})


class AdminDocsPage(AdminPage):
  def get(self):
    data = {}

    writers = ""
    for writer in model.doc.DocWriter.all():
      if writers:
        writers += ", "
      writers += writer.user.email()
    data["writers"] = writers

    self.render('admin/docs/index.html', data)


class AdminDocsWritersPage(AdminPage):
  def post(self):
    data = {}

    for writer in model.doc.DocWriter.all():
      writer.delete()

    for email in self.request.POST.get("writers").split(","):
      email = email.strip()
      user = users.User(email)
      writer = model.doc.DocWriter(user=user)
      writer.put()
    memcache.delete("docs:writers")

    self.redirect('/admin/docs')


app = webapp.WSGIApplication([('/admin', AdminDashboardPage),
                              ('/admin/posts', AdminPostListPage),
                              ('/admin/posts/([0-9]+|new)', AdminPostsPage),
                              ('/admin/posts/([0-9]+)/delete', AdminPostDeletePage),
                              ('/admin/blobs', AdminBlobsPage),
                              ('/admin/blobs/new', AdminBlobsNewPage),
                              ('/admin/docs', AdminDocsPage),
                              ('/admin/docs/writers', AdminDocsWritersPage)],
                             debug=os.environ['SERVER_SOFTWARE'].startswith('Development'))
