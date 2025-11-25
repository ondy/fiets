function getEditFilterModal() {
  if (typeof bootstrap === 'undefined') {
    return null;
  }
  var modalElement = document.getElementById('edit-filter-modal');
  if (!modalElement) {
    return null;
  }
  return bootstrap.Modal.getOrCreateInstance(modalElement);
}

function updateUnreadCount(unread) {
  var $unread = $('.unread-count');
  if ($unread.length === 0) {
    return;
  }
  $unread.text(unread);
  var $doc = $(document);
  var title = $doc.attr("title");
  if (title.indexOf(' of ') > 0) {
    title = title.replace(/of \d+ posts/, 'of ' + unread + ' posts');
  } else {
    title = title.replace(/\d+ posts/, unread + ' posts');
  }
  $doc.attr("title", title);
}

function initPostPager() {
  var $postList = $('.posts-list');
  var $markRead = $('.mark-read-action');
  if ($postList.length === 0 || $markRead.length === 0) {
    return;
  }

  var pageSize = parseInt($postList.data('page-size'), 10) || 20;
  var totalUnread = parseInt($markRead.data('total-unread'), 10);
  if (isNaN(totalUnread)) {
    totalUnread = $postList.find('.post').length;
  }

  var postsCache = $postList.find('.post').detach().toArray();

  function visiblePosts() {
    return postsCache.slice(0, pageSize);
  }

  function updateMarkReadLink() {
    var postsToMark = visiblePosts();
    if (postsToMark.length === 0) {
      $postList.append('<li class="list-group-item"><small>No more posts.</small></li>');
      $markRead.addClass('disabled').attr('aria-disabled', 'true');
      $markRead.find('small').text('No more posts to mark');
      return;
    }

    var ids = postsToMark.map(function (post) {
      return $(post).data('post-id');
    });

    $markRead
      .removeClass('disabled')
      .removeAttr('aria-disabled')
      .attr('href', '/markread?posts=' + ids.join(','))
      .find('small')
      .text('Mark ' + postsToMark.length + ' of ' + totalUnread + ' read');
  }

  function renderVisiblePosts() {
    $postList.empty();
    visiblePosts().forEach(function (post) {
      $postList.append(post);
    });
    updateMarkReadLink();
  }

  renderVisiblePosts();

  $markRead.on('click', function (evt) {
    var postsToMark = visiblePosts();
    if (postsToMark.length === 0) {
      return;
    }
    evt.preventDefault();

    var ids = postsToMark.map(function (post) {
      return $(post).data('post-id');
    });

    $.ajax({
      url: '/markread?posts=' + ids.join(','),
      cache: false
    })
    .fail(function(jqXHR, textStatus, errorThrown) {
      console.log(textStatus + " - " + errorThrown);
      alert(textStatus);
    });

    postsCache = postsCache.slice(postsToMark.length);
    totalUnread = Math.max(totalUnread - postsToMark.length, 0);
    updateUnreadCount(totalUnread);
    renderVisiblePosts();
  });
}

setInterval(function () {
  $.ajax({
    dataType: "json",
    url: "/counts",
    cache: false
  })
  .done(function (data, textStatus, jqXHR) {
    updateUnreadCount(data.unread_count);
  })
  .fail(function(jqXHR, textStatus, errorThrown) {
    console.log(textStatus + " - " + errorThrown);
    alert(textStatus);
  });
}, 30000);

$('body')
.on('click', '.post-actions .add-bookmark,.post-actions .remove-bookmark', function (evt) {
  evt.preventDefault();
  var $link = $(this);
  var target = $link.attr('href');
  var $post = $link.closest('.post');
  $.ajax({
    url: target,
    cache: false
  })
  .done(function (data, textStatus, jqXHR) {
    var $bookmarkCount = $('.bookmark-count');
    var count = parseInt($bookmarkCount.text());
    if ($link.hasClass('add-bookmark')) {
      $post.addClass('bookmarked');
      $bookmarkCount.text(count+1);
    } else if ($link.hasClass('remove-bookmark')) {
      $post.removeClass('bookmarked');
      $bookmarkCount.text(count-1);
    }
  })
  .fail(function(jqXHR, textStatus, errorThrown) {
    console.log(textStatus + " - " + errorThrown);
    alert(textStatus);
  });
})
.on('click', '.post-actions .add-filter', function (evt) {
  var link = $(this).closest('.post').find("h3 a");
  var url = link.attr('href');
  var title = link.closest('h3').attr('title');
  $('#filter-url').val(url);
  $('#filter-title').val(title);
  $('#add-filter').show();
  $('#edit-filter').hide();
  var modal = getEditFilterModal();
  if (modal) {
    modal.show();
  }
})
.on('click', '.filter-actions .edit-filter', function (evt) {
  var row = $(this).closest('ul');
  var id = row.data('id');
  var url = row.find('.url').val();
  var urlMatch = row.find('.url-match').val();
  var title = row.find('.title').val();
  var titleMatch = row.find('.title-match').val();
  $('#filter-id').val(id);
  $('#filter-url').val(url);
  $('#filter-url-match').val(urlMatch);
  $('#filter-title').val(title);
  $('#filter-title-match').val(titleMatch);
  $('#edit-filter').show();
  $('#add-filter').hide();
  var modal = getEditFilterModal();
  if (modal) {
    modal.show();
  }
})
;

$('#add-filter').click(function () {
  var form = $('#edit-filter-modal form');
  $.ajax({
     type: 'POST',
     url: '/add-filter',
     data: form.serialize()
  })
  .done(function (data, textStatus, jqXHR) {
      var modal = getEditFilterModal();
      if (modal) {
        modal.hide();
      }
  })
  .fail(function(jqXHR, textStatus, errorThrown) {
    console.log(textStatus + " - " + errorThrown);
    alert(textStatus);
  });
});
$('#edit-filter').click(function () {
  var form = $('#edit-filter-modal form');
  $.ajax({
     type: 'POST',
     url: '/edit-filter',
     data: form.serialize()
  })
  .done(function (data, textStatus, jqXHR) {
      var modal = getEditFilterModal();
      if (modal) {
        modal.hide();
      }
   })
   .fail(function(jqXHR, textStatus, errorThrown) {
     console.log(textStatus + " - " + errorThrown);
     alert(textStatus);
   });
});
$('.bookmarklet').each(function () {
  var $a = $(this);
  var href = $a.attr("href");
  var url = window.location.href;
  var pos = url.indexOf('://') + 3;
  pos = url.indexOf('/', pos);
  href = href.replace("*HOST*", url.substring(0, pos));
  $a.attr("href", href);
});

$(function () {
  initPostPager();
});
