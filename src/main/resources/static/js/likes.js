/**
 * Shared logic for dynamic "Like" (Wishlist) functionality.
 */
document.addEventListener('DOMContentLoaded', () => {
    initLikeButtons();
});

/**
 * Initializes all like buttons on the page.
 */
function initLikeButtons() {
    const likeButtons = document.querySelectorAll('.like-button:not(.initialized)');

    likeButtons.forEach(button => {
        button.classList.add('initialized');
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            e.stopPropagation();

            const productId = button.getAttribute('data-product-id');
            if (!productId) return;

            // Disable button during request
            button.disabled = true;

            try {
                console.log('Toggling like for product:', productId);

                const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

                const response = await fetch(`/like/toggle/${productId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    }
                });

                const result = await response.text();
                console.log('Result:', result);

                if (result === 'liked') {
                    updateLikeUI(button, true);
                    console.log('✅ Product liked');
                } else if (result === 'unliked') {
                    updateLikeUI(button, false);
                    console.log('✅ Product unliked');
                } else if (result.startsWith('error:not_logged_in')) {
                    window.location.href = '/login';
                } else if (result.startsWith('error')) {
                    alert('Error: ' + result.split(':')[1]);
                }
            } catch (error) {
                console.error('Error toggling like:', error);
                alert('Failed to toggle like. Please try again.');
            } finally {
                // Re-enable button
                button.disabled = false;
            }
        });
    });
}

/**
 * Updates the UI of a specific like button based on its new state.
 */
function updateLikeUI(button, liked) {
    const svg = button.querySelector('svg');
    const textElement = button.querySelector('.like-text');

    if (liked) {
        if (svg) {
            svg.setAttribute('fill', 'currentColor');
            svg.classList.add('text-red-500');
        }
        button.classList.add('text-red-500');
        button.classList.add('active-like');
        if (textElement) textElement.innerText = 'Liked';

        // Specific handling for detail page button
        if (button.id === 'likeButton') {
            button.classList.add('border-red-200', 'bg-red-50');
        }
    } else {
        if (svg) {
            svg.setAttribute('fill', 'none');
            svg.classList.remove('text-red-500');
        }
        button.classList.remove('text-red-500');
        button.classList.remove('active-like');
        if (textElement) textElement.innerText = 'Add to Wishlist';

        // Specific handling for detail page button
        if (button.id === 'likeButton') {
            button.classList.remove('border-red-200', 'bg-red-50');
        }
    }
}
