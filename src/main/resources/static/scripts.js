setInterval(function () {
  $.ajax({
    dataType: "json",
    url: "/counts",
    cache: false
  })
  .done(function (data, textStatus, jqXHR) {
    var unread = data.unread_count;
    var $unread = $('.unread-count');
    if ($unread.length > 0) {
      $('.unread-count').text(unread);
      var $doc = $(document);
      var title = $doc.attr("title");
      if (title.indexOf(' of ') > 0) {
        title = title.replace(/of \d+ posts/, 'of ' + unread + ' posts');
      } else {
        title = title.replace(/\d+ posts/, unread + ' posts');
      }
      $doc.attr("title", title);
    }
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
  var title = link.attr('title');
  $('#filter-url').val(url);
  $('#filter-title').val(title);
  $('#add-filter').show();
  $('#edit-filter').hide();
  $('#edit-filter-modal').modal();
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
  $('#edit-filter-modal').modal();
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
      $('#edit-filter-modal').modal('hide');
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
      $('#edit-filter-modal').modal('hide');
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
