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
.on('click', '.post-actions a', function (evt) {
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
    if ($link.hasClass('add-bookmark-link')) {
      $post.addClass('bookmarked');
      $bookmarkCount.text(count+1);
    } else if ($link.hasClass('remove-bookmark-link')) {
      $post.removeClass('bookmarked');
      $bookmarkCount.text(count-1);
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
  href = href.replace("%HOST%", url.substring(0, pos+1));
  $a.attr("href", href);
});
